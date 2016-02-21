package alexclin.httplite.url.cache;

import java.io.File;
import java.io.IOException;

import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.url.CacheDispatcher;
import alexclin.httplite.url.URLCache;

/**
 * URLCacheImpl
 */
public class UrlCache implements URLCache{

    private static final int MAX_CONTENT_LENGTH = 5 * 1024 * 1024;
    private static final int APP_VERSION = 1;
    private static final int DEFAULT_POOL_SIZE = 4096;

    private DiskLruCache cache;
    private ByteArrayPool pool;

    public UrlCache(File directory, long maxSize,ByteArrayPool pool) throws IOException{
        cache = DiskLruCache.open(directory,APP_VERSION,2,maxSize,2048);
        this.pool = pool;
    }

    public UrlCache(File directory, long maxSize) throws IOException{
        this(directory,maxSize,new ByteArrayPool(DEFAULT_POOL_SIZE));
    }

    @Override
    public Response get(Request request) throws IOException {
        String key = CacheDispatcher.getCacheKey(request);
        DiskLruCache.Snapshot snapshot = cache.get(key);
        CacheEntry entry = CacheEntryParser.newEntry(snapshot,pool,request);
        if(entry.isExpired()) return null;
        return entry.getResponse();
    }

    @Override
    public void remove(Request request) throws IOException {
        String key = CacheDispatcher.getCacheKey(request);
        cache.remove(key);
    }

    @Override
    public void put(Response response) throws IOException {
        if(response.body().contentLength()>MAX_CONTENT_LENGTH||response.body().contentLength()>cache.getMaxSize()/3){
            return;
        }
        String cacheKey = CacheDispatcher.getCacheKey(response.request());
        DiskLruCache.Snapshot snapshot = cache.get(cacheKey);
        if(snapshot==null) return;
        DiskLruCache.Editor editor = null;
        try {
            editor = snapshot.edit();
            CacheEntry entry = CacheEntryParser.parseCacheEntry(response);
            CacheEntryParser.writeEntryTo(entry,editor);
            editor.commit();
        } catch (IOException e) {
            abortQuietly(editor);
        }
    }

    public void addCacheHeaders(Request request){
        CacheEntry entry = null;
        try {
            DiskLruCache.Snapshot snapshot = cache.get(CacheDispatcher.getCacheKey(request));
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
            String lastModified = CacheEntryParser.foramtDateAsEpoch(entry.getLastModified());
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
