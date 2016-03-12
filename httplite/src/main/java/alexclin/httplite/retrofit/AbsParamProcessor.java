package alexclin.httplite.retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * AbsParamProcessor
 *
 * @author alexclin 16/1/31 13:45
 */
interface AbsParamProcessor {
    boolean support(Annotation annotation);
    void checkParameters(Method method,Annotation annotation,Type parameterType) throws RuntimeException;
}
