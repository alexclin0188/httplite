package alexclin.httplite.url.cache;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.impl.ResponseImpl;
import alexclin.httplite.util.LogUtil;

/**
 * URLCacheImpl
 */
public class CacheImpl{

    private static final int MAX_CONTENT_LENGTH = 5 * 1024 * 1024;
    private static final int APP_VERSION = 1;
    private static final int DEFAULT_POOL_SIZE = 4096;

    private DiskLruCache cache;
    private ByteArrayPool pool;
    private CachePolicy cachePolicy;

    public CacheImpl(File directory, long maxSize, ByteArrayPool pool,CachePolicy cachePolicy) throws IOException{
        cache = DiskLruCache.open(directory,APP_VERSION,2,maxSize,2048);
        this.pool = pool;
        this.cachePolicy = cachePolicy;
    }

    public CacheImpl(File directory, long maxSize,CachePolicy cachePolicy) throws IOException{
        this(directory,maxSize,new ByteArrayPool(DEFAULT_POOL_SIZE),cachePolicy);
    }

    public Response get(Request request,boolean force) throws IOException {
        String key = cachePolicy.createCacheKey(request);
        DiskLruCache.Snapshot snapshot = cache.get(key);
        if(snapshot==null){
            return null;
        }
        CacheEntry entry = CacheParser.newEntry(snapshot, pool, request);
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

    public boolean remove(Request request) throws IOException {
        String key = cachePolicy.createCacheKey(request);
        cache.remove(key);
        return true;
    }

    public Response put(Response response) throws IOException {
        if(response.body().contentLength()>MAX_CONTENT_LENGTH||response.body().contentLength()>cache.getMaxSize()/3){
            return response;
        }
        byte[] data = IOUtil.readAllBytes(response.body().stream());
        Response returnResponse = new ResponseImpl(response,new CacheParser.PoolingStream(data,pool),data.length);

        CacheEntry entry = CacheParser.parseCacheEntry(response,new CacheParser.PoolingStream(data,pool),data.length);
        if(entry==null){
            return returnResponse;
        }
        String cacheKey = cachePolicy.createCacheKey(response.request());
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
            CacheParser.writeEntryTo(entry, editor);
            editor.commit();
        } catch (Exception e){
            abortQuietly(editor);
            throw e;
        }
        return returnResponse;
    }

    public void addCacheHeaders(Request request,Map<String, List<String>> headers){
        CacheEntry entry = null;
        try {
            DiskLruCache.Snapshot snapshot = cache.get(cachePolicy.createCacheKey(request));
            if(snapshot==null){
                return;
            }
            entry = CacheParser.newEntry(snapshot);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // If there's no cache entry, we're done.
        if (entry == null) {
            return;
        }

        if (entry.getEtag() != null) {
            addHeader(headers,"If-None-Match", entry.getEtag());
        }

        if (entry.getLastModified() > 0) {
            String lastModified = CacheParser.formatDateAsEpoch(entry.getLastModified());
            if(lastModified!=null)
                addHeader(headers,"If-Modified-Since", lastModified);
        }
    }

    private static void addHeader(Map<String, List<String>> headers,String key,String value){
        List<String> list = headers.get(key);
        if(list==null){
            list = new ArrayList<>();
        }else if(!(list instanceof ArrayList)){
            list = new ArrayList<>(list);
        }
        list.add(value);
        headers.put(key,list);
    }

    public String createCacheKey(Request request){
        return cachePolicy.createCacheKey(request);
    }

    public boolean canCache(Request request){
        return cachePolicy.canCache(request);
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
