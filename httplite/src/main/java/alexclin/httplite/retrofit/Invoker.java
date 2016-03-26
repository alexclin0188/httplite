package alexclin.httplite.retrofit;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Call;

/**
 * Invoker
 *
 * @author alexclin  16/3/24 23:24
 */
public interface Invoker {
    Object invoke(Call call,Type returnType,Object... args) throws Exception;
    boolean support(Method method);

    /**
     * check method defined in interface and return whether the result want to parse is File
     * @param method method
     * @return whether the result want to parse is File
     * @throws RuntimeException
     */
    boolean checkMethod(Method method) throws RuntimeException;
}
