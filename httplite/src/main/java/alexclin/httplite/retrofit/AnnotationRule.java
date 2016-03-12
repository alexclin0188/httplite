package alexclin.httplite.retrofit;

import java.lang.reflect.Method;

/**
 * AnnotationRule
 *
 * 检查接口中函数中的Annoation使用和参数是否复合规则，在此接口中实现规则定义
 *
 * @author alexclin 16/1/30 09:54
 */
public interface AnnotationRule {
    void checkMethod(Method method) throws RuntimeException;
}
