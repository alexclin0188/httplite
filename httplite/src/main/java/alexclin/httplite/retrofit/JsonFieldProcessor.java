package alexclin.httplite.retrofit;

import android.text.TextUtils;
import android.util.Pair;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import alexclin.httplite.Request;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

class JsonFieldProcessor implements ParamMiscProcessor{

    @Override
    public void process(Request.Builder request, Annotation[][] annotations, List<Pair<Integer,Integer>> list, Object... args) {
        try {
            Object jsonObject = null;
            for(Pair<Integer,Integer> pair:list){
                int argPos = pair.first;
                int annotationPos = pair.second;
                if(args[argPos]==null) continue;
                JsonField annotation = (JsonField) annotations[argPos][annotationPos];
                String key = annotation.value();
                Object arg = args[argPos];
                if(TextUtils.isEmpty(key)){
                    jsonObject = fillJsonWithObject(jsonObject,arg);
                    break;
                }else{
                    if(jsonObject==null){
                        jsonObject = new JSONObject();
                    }
                    if(Util.isBasicTypeBean(arg)||(arg instanceof JSONObject)||(arg instanceof JSONArray)){
                        ((JSONObject)jsonObject).put(key,arg);
                    }else if(arg.getClass().isArray()){
                        ((JSONObject)jsonObject).put(key,toJsonArray(arg));
                    }else if(arg instanceof Collection){
                        ((JSONObject)jsonObject).put(key,new JSONArray((Collection<?>)arg));
                    }else if(arg instanceof Serializable){
                        ((JSONObject)jsonObject).put(key,convertToJson((Serializable) arg));
                    }
                }
            }
            if(jsonObject != null){
                request.body(MediaType.APPLICATION_JSON,jsonObject.toString());
            }
        } catch (JSONException e) {
            LogUtil.e("HandleJsonFields error:",e);
        }
    }

    @Override
    public void checkMiscParameters(Method method,Annotation[][] annotations, List<Pair<Integer, Integer>> list, Type... argTypes) {
        int noValueArray = 0;
        int noValueBean = 0;
        int valueField = 0;
        for(Pair<Integer,Integer> pair:list){
            int argPos = pair.first;
            int annotationPos = pair.second;
            JsonField annotation = (JsonField) annotations[argPos][annotationPos];
            String key = annotation.value();
            Type argType = argTypes[argPos];
            if(TextUtils.isEmpty(key)){
                if((argType instanceof Class)&&((Class<?>)argType).isArray()){
                    noValueArray++;
                }else if(Util.isSubType(argType,Collection.class)||argType==JSONArray.class){
                    noValueArray++;
                }else{
                    noValueBean++;
                }
            }else{
                valueField++;
            }
        }
        if(valueField>1){
            valueField = 1;
        }
        //如果有无参的，除此之外不应该再有其他参数
        if(noValueArray+noValueBean+valueField>1){
            throw Util.methodError(method, "Empty value with @JsonField param cannot exist with other param(whatever with value or not)");
        }
    }

    @Override
    public boolean support(Annotation annotation) {
        return annotation instanceof JsonField;
    }

    @Override
    public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
        if(!jsonSupportType(parameterType)){
            throw Util.methodError(method,"Annotation @JsonField only support parameter type String/JSONObject/JSONArray/Boolean/Number/int/long/double/short/Serializable and all those type's array");
        }
        if(TextUtils.isEmpty(((JsonField)annotation).value())&&!Util.isBasicType(parameterType)){
            throw Util.methodError(method,"The annotation {@JsonField(value)} for basic type params(eg. int/long/...) must not be empty");
        }
    }

    private static boolean jsonSupportType(Type type){
        if((type instanceof Class)&&((Class<?>)type).isArray()){
            return jsonSupportType(((Class<?>)type).getComponentType());
        }
        return Util.isBasicType(type) || type == JSONObject.class || type == JSONArray.class || Util.isSubType(type, Serializable.class);
    }

    private static Object fillJsonWithObject(Object jsonObject,Object bean){
        if(bean instanceof JSONObject){
            if(jsonObject==null){
                return bean;
            }else if(jsonObject instanceof JSONObject){
                return combineJSONObject((JSONObject) jsonObject,(JSONObject) bean);
            }else{
                //TODO 启动时就该抛异常
                throw new RuntimeException("无value的@JsonField注解和有value的不能同时使用, 且无value的@JsonField注解只能存在一个");
            }
        }else if(bean instanceof JSONArray){
            if(jsonObject==null){
                return bean;
            }else if(jsonObject instanceof JSONArray){
                return combineJSONArray((JSONArray) jsonObject,(JSONArray) bean);
            }else if(jsonObject instanceof JSONObject){
                //TODO 启动时就该抛异常
                throw new RuntimeException("无value的@JsonField注解和有value的不能同时使用, 且无value的@JsonField注解只能存在一个");
            }
        }else if(bean.getClass().isArray()||(bean instanceof Collection)){
            JSONArray array;
            if (bean.getClass().isArray()){
                array = toJsonArray(bean);
            }else{
                array  = new JSONArray((Collection<?>)bean);
            }
            if(jsonObject==null){
                return array;
            }else if(jsonObject instanceof JSONArray){
                return combineJSONArray((JSONArray) jsonObject,array);
            }else{
                //TODO 启动时就该抛异常
                throw new RuntimeException("无value的@JsonField注解和有value的不能同时使用, 且无value的@JsonField注解只能存在一个");
            }
        }else if(!Util.isBasicTypeBean(bean)&&(bean instanceof Serializable)){
            JSONObject beanJson = convertToJson((Serializable) bean);
            if(jsonObject==null){
                return beanJson;
            }else if(jsonObject instanceof JSONObject){
                return combineJSONObject((JSONObject) jsonObject,beanJson);
            }else if(jsonObject instanceof JSONArray){
                //TODO 启动时就该抛异常
                throw new RuntimeException("无value的@JsonField注解和有value的不能同时使用, 且无value的@JsonField注解只能存在一个");
            }
        }
        return jsonObject;
    }

    private static JSONArray toJsonArray(Object bean) {
        JSONArray array;
        array = new JSONArray();
        int length = Array.getLength(bean);
        for(int i=0;i<length;i++){
            array.put(Array.get(bean,i));
        }
        return array;
    }

    private static JSONObject combineJSONObject(JSONObject json1,JSONObject json2){
        Iterator<String> iterator = json2.keys();
        while (iterator.hasNext()){
            String key = iterator.next();
            try {
                json1.put(key,json2.get(key));
            } catch (JSONException e) {
                LogUtil.e("combineJSONObject get for "+key+" error",e);
            }
        }
        return json1;
    }

    private static JSONArray combineJSONArray(JSONArray array1,JSONArray array2){
        for(int i=0;i<array2.length();i++){
            try {
                array1.put(array2.get(i));
            } catch (JSONException e) {
                LogUtil.e("combineJSONArray get index "+i+" error",e);
            }
        }
        return null;
    }

    private static JSONObject convertToJson(Serializable serializable){
        JSONObject jsonObject = new JSONObject();

        return null;
    }
}