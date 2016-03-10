package alexclin.httplite.retrofit;

import java.lang.annotation.Annotation;

import alexclin.httplite.Request;

/**
 * MethodProcessor
 *
 * @author alexclin
 * @date 16/1/28 19:18
 */
public interface MethodProcessor {
    void process(Annotation annotation,Retrofit retrofit, Request request);
    boolean support(Annotation annotation);
}
