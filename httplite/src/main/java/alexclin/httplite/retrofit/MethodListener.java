package alexclin.httplite.retrofit;

import java.lang.reflect.Method;

/**
 * MethodListener
 *
 * @author alexclin 16/1/31 00:05
 */
public interface MethodListener {
    void onMethod(Method method,Retrofit retrofit,Object... args);
}
