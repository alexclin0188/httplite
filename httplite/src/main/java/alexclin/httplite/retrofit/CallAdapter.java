package alexclin.httplite.retrofit;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;

/**
 * CallAdapter
 *
 * @author alexclin  16/3/24 23:24
 */
public interface CallAdapter {
    interface RequestCreator{
        Request createRequest(Object[] args);
    }

    enum ResultType{File,NotFile}
    Object adapt(HttpLite lite, RequestCreator creator, Type returnType, Object[] args) throws Exception;
    boolean support(Method method);

    /**
     * check method defined in interface and return whether the result want to parse is File
     * @param method method
     * @return whether the result want to parse is File
     * @throws RuntimeException exception
     */
    ResultType checkMethod(Method method) throws RuntimeException;
}
