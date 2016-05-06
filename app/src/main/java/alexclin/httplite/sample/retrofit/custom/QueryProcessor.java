package alexclin.httplite.sample.retrofit.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Request;
import alexclin.httplite.retrofit.ParameterProcessor;

/**
 * QueryProcessor
 *
 * @author alexclin  16/5/5 23:18
 */
public class QueryProcessor implements ParameterProcessor {
    @Override
    public void process(Annotation annotation, Request request, Object value) {

    }

    @Override
    public boolean support(Annotation annotation) {
        return false;
    }

    @Override
    public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {

    }
}
