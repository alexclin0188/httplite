package alexclin.httplite.url;

import java.io.IOException;

import alexclin.httplite.Request;
import alexclin.httplite.Response;

/**
 * URLCache
 *
 * @author alexclin
 * @date 16/2/17 20:41
 */
public interface URLCache {
    Response get(Request request) throws IOException;
    void remove(Request request) throws IOException;
    void put(Response response) throws IOException;
    void addCacheHeaders(Request request);
}
