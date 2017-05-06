package alexclin.httplite.url.cache;

import alexclin.httplite.Request;

/**
 * Cache
 *
 * @author alexclin  16/4/7 21:46
 */
public interface CacheHandler {
    String createCacheKey(Request request);
    boolean canCache(Request request);
}
