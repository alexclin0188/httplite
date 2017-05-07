package alexclin.httplite.sample.retrofit.custom;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Request;
import alexclin.httplite.annotation.Param;
import alexclin.httplite.retrofit.ParameterProcessor;
import alexclin.httplite.retrofit.ProcessorFactory;

/**
 * QueryProcessor
 *
 * @author alexclin  16/5/5 23:18
 */
public class QueryProcessor extends ProcessorFactory.ObjectsProcessor {

    @Override
    protected void performProcess(Annotation annotation, Request.Builder request, Object value) {
        request.param(((Query)annotation).value(),value.toString(),((Query)annotation).encoded());
    }

    @Override
    protected String value(Annotation annotation) {
        return ((Query)annotation).value();
    }

    @Override
    public boolean support(Annotation annotation) {
        return annotation instanceof Query;
    }
}
