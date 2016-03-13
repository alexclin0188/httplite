package alexclin.httplite.url.cache;

import java.io.File;
import java.io.IOException;

import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.internal.ResponseImpl;
import alexclin.httplite.url.CacheDispatcher;
import alexclin.httplite.url.URLCache;
import alexclin.httplite.util.LogUtil;

/**
 * URLCacheImpl
 */
public class CacheImpl implements URLCache{

    private static final int MAX_CONTENT_LENGTH = 5 * 1024 * 1024;
    private static final int APP_VERSION = 1;
    private static final int DEFAULT_POOL_SIZE = 4096;

    private DiskLruCache cache;
    private ByteArrayPool pool;

    public CacheImpl(File directory, long maxSize, ByteArrayPool pool) throws IOException{
        cache = DiskLruCache.open(directory,APP_VERSION,2,maxSize,2048);
        this.pool = pool;
    }

    public CacheImpl(File directory, long maxSize) throws IOException{
        this(directory,maxSize,new ByteArrayPool(DEFAULT_POOL_SIZE));
    }

    @Override
    public Response get(Request request,boolean force) throws IOException {
        String key = CacheDispatcher.getCacheKey(request);
        DiskLruCache.Snapshot snapshot = cache.get(key);
        if(snapshot==null){
            return null;
        }
        CacheEntry entry = CacheEntryParser.newEntry(snapshot, pool, request);
        if (force||request.getCacheExpiredTime() == Request.FORCE_CACHE) {
            return entry.getResponse();
        }
        boolean isEntryExpired = entry.isExpired();
        boolean isRequestExpired = (request.getCacheExpiredTime() > 0 && entry.getLastModified() + request.getCacheExpiredTime() * 1000 > System.currentTimeMillis());
        if (isEntryExpired||isRequestExpired) {
            LogUtil.i(String.format("Expired %b-%b, remove old:%s",isEntryExpired,isRequestExpired,entry));
            cache.remove(key);
            return null;
        }
        return entry.getResponse();
    }

    @Override
    public boolean remove(Request request) throws IOException {
        String key = CacheDispatcher.getCacheKey(request);
        cache.remove(key);
        return true;
    }

    @Override
    public Response put(Response response) throws IOException {
        if(response.body().contentLength()>MAX_CONTENT_LENGTH||response.body().contentLength()>cache.getMaxSize()/3){
            return response;
        }
        byte[] data = IOUtil.readAllBytes(response.body().stream());
        Response returnResponse = new ResponseImpl(response,new CacheEntryParser.PoolingStream(data,pool),data.length);

        CacheEntry entry = CacheEntryParser.parseCacheEntry(response,new CacheEntryParser.PoolingStream(data,pool),data.length);
        if(entry==null){
            return returnResponse;
        }
        String cacheKey = CacheDispatcher.getCacheKey(response.request());
        DiskLruCache.Snapshot snapshot = cache.get(cacheKey);
        DiskLruCache.Editor editor;
        if (snapshot == null){
            editor = cache.edit(cacheKey);
        }else{
            editor = snapshot.edit();
        }
        if(editor==null){
            LogUtil.e("edit failed");
            return returnResponse;
        }
        try {
            CacheEntryParser.writeEntryTo(entry, editor);
            editor.commit();
        } catch (Exception e){
            abortQuietly(editor);
            throw e;
        }
        return returnResponse;
    }

    public void addCacheHeaders(Request request){
        CacheEntry entry = null;
        try {
            DiskLruCache.Snapshot snapshot = cache.get(CacheDispatcher.getCacheKey(request));
            if(snapshot==null){
                return;
            }
            entry = CacheEntryParser.newEntry(snapshot);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If there's no cache entry, we're done.
        if (entry == null) {
            return;
        }

        if (entry.getEtag() != null) {
            request.header("If-None-Match", entry.getEtag());
        }

        if (entry.getLastModified() > 0) {
            String lastModified = CacheEntryParser.formatDateAsEpoch(entry.getLastModified());
            if(lastModified!=null)
                request.header("If-Modified-Since", lastModified);
        }
    }

    private void abortQuietly(DiskLruCache.Editor editor) {
        // Give up because the cache cannot be written.
        try {
            if (editor != null) {
                editor.abort();
            }
        } catch (IOException ignored) {
        }
    }
}
