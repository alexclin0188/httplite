package alexclin.httplite.retrofit;

import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.RequestFilter;

/**
 * AsyncCallAdapter
 *
 * @author alexclin  16/3/18 22:44
 */
public class AsyncCallAdapter implements CallAdapter<Handle> {
    @Override
    public Handle adapt(HttpLite lite, RequestFilter filter, Object... args) {
        return null;
    }
}
