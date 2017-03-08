package alexclin.httplite.listener;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;

/**
 * Callback
 *
 * @author alexclin 16/1/1 10:27
 */
public interface Callback<T> {
    void onSuccess(Request req,Map<String,List<String>> headers,T result);
    void onFailed(Request req, Exception e);
}
