package alexclin.httplite.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import alexclin.httplite.Request;

/**
 * MethodProcessor
 *
 * @author alexclin 16/1/28 19:18
 */
public interface MethodProcessor {
    void process(Method method,Annotation annotation, Retrofit retrofit, Request.Builder originRequest);
    boolean support(Annotation annotation);
}