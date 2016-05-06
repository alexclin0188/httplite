package alexclin.httplite.sample.retrofit.custom;

import android.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import alexclin.httplite.Request;
import alexclin.httplite.retrofit.ParamMiscProcessor;

/**
 * GsonFieldProcesscor
 *
 * @author alexclin  16/5/5 23:12
 */
public class GsonFieldProcesscor implements ParamMiscProcessor {
    public static final String BODY_TYPE = "gson_json_body";

    @Override
    public void process(Request request, Annotation[][] annotations, List<Pair<Integer, Integer>> list, Object... args) {
        //处理所有带有Gson注解的参数，list中存储的是所有Gson注解的位置

    }

    @Override
    public boolean support(Annotation annotation) {
        return annotation instanceof GsonField;
    }

    @Override
    public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
        //在此函数中检查参数类型是否定义正确
    }
}
