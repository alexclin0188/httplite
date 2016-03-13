package alexclin.httplite.url;

import java.io.IOException;

import alexclin.httplite.Request;
import alexclin.httplite.Response;

/**
 * URLCache
 *
 * @author alexclin 16/2/17 20:41
 */
public interface URLCache {
    Response get(Request request,boolean force) throws IOException;
    boolean remove(Request request) throws IOException;
    Response put(Response response) throws IOException;
    void addCacheHeaders(Request request);
}
