package alexclin.httplite.listener;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.Response;

/**
 * ResponseFilter
 *
 * @author alexclin 16/1/20 22:34
 */
public interface ResponseFilter {
    void onResponse(HttpLite lite,Request request,Response response);
}
