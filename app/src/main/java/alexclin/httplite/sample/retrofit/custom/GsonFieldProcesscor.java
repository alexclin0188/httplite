package alexclin.httplite.sample.retrofit.custom;

import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import alexclin.httplite.listener.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.retrofit.ParamMiscProcessor;
import alexclin.httplite.util.Util;

/**
 * GsonFieldProcesscor
 *
 * @author alexclin  16/5/5 23:12
 */
public class GsonFieldProcesscor implements ParamMiscProcessor {
    public static final String BODY_TYPE = "gson_json_body";

    @Override
    public void process(Request.Builder request, Annotation[][] annotations, List<Pair<Integer, Integer>> list, Object... args) {
        //处理所有带有Gson注解的参数，list中存储的是所有Gson注解的位置
        JsonObject object = new JsonObject();
        for(Pair<Integer,Integer> pair:list){
            int argPos = pair.first;
            int annotationPos = pair.second;
            if(args[argPos]==null) continue;
            GsonField annotation = (GsonField) annotations[argPos][annotationPos];
            String key = annotation.value();
            if(args[argPos] instanceof String){
                object.addProperty(key,(String)args[argPos]);
            }else if(args[argPos] instanceof JsonElement){
                object.add(key,(JsonElement)args[argPos]);
            }else if(args[argPos] instanceof Boolean){
                object.addProperty(key,(Boolean)args[argPos]);
            }else if(args[argPos] instanceof Number){
                object.addProperty(key,(Number)args[argPos]);
            }
        }
        request.body(MediaType.APPLICATION_JSON,object.toString());
    }

    @Override
    public boolean support(Annotation annotation) {
        return annotation instanceof GsonField;
    }

    @Override
    public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
        //在此函数中检查参数类型是否定义正确
        if(!gsonSupportType(parameterType)){
            throw Util.methodError(method,"Annotation @GsonField only support parameter type String/JsonElement/Boolean/Number/int/long/double/short");
        }if(TextUtils.isEmpty(((GsonField)annotation).value())){
            throw Util.methodError(method,"The annotation {@GsonField(value) value} must not be null");
        }
    }

    private boolean gsonSupportType(Type type){
        return type==String.class || Util.isSubType(type,JsonElement.class) || type == int.class || type == long.class || type == double.class
                || type == short.class || Util.isSubType(type,Number.class) || type == boolean.class || type == Boolean.class;
    }
}
