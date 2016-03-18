package alexclin.httplite.retrofit;

import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.RequestFilter;

/**
 * SyncCallAdapter
 *
 * @author alexclin  16/3/18 22:45
 */
public class SyncCallAdapter implements CallAdapter<Object> {
    @Override
    public Object adapt(HttpLite lite, RequestFilter filter, Object... args) {
        return null;
    }
}
