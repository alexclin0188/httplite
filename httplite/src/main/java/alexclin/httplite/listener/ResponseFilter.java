package alexclin.httplite.listener;

import alexclin.httplite.Request;
import alexclin.httplite.Response;

/**
 * ResponseFilter
 *
 * @author alexclin
 * @date 16/1/20 22:34
 */
public interface ResponseFilter {
    void onResponse(Request request,Response response);
}
