package alexclin.httplite.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Request;

/**
 * ParameterProcessor
 *
 * @author alexclin
 * @date 16/1/27 22:04
 */
public interface ParameterProcessor extends AbsParamProcessor{
    void process(Annotation annotation,Request request,Object value);
}
