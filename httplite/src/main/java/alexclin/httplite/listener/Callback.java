package alexclin.httplite.listener;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 10:27
 */
public interface Callback<T> {
    void onSuccess(T result,Map<String,List<String>> headers);
    void onFailed(Request req, Exception e);
}
