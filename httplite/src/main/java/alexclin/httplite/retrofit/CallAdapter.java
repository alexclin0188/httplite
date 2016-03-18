package alexclin.httplite.retrofit;

import java.lang.reflect.Type;

import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.RequestFilter;

/**
 * CallAdapter
 *
 * @author alexclin  16/3/18 22:42
 */
public interface CallAdapter<T> {
    T adapt(HttpLite lite,RequestFilter filter,Object... args);
}
