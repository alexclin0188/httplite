package alexclin.httplite.listener;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.ResultCallback;

/**
 * RequestFilter
 *
 * @author alexclin 16/1/18 21:37
 */
public interface RequestFilter {
    void onRequest(HttpLite lite,Request request, Type resultType);
}
