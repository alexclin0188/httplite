package alexclin.httplite.retrofit;

import android.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.util.Util;

/**
 * MethodHandler
 *
 * @author alexclin 16/1/28 19:20
 */
public class MethodHandler<T> {
    private MethodProcessor[] methodProcessors;
    private ParameterProcessor[][] parameterProcessors;
    private Type returnType;
    private Annotation[][] methodParameterAnnotationArrays;
    private Annotation[] methodAnnotations;
    private Map<ParamMiscProcessor,List<Pair<Integer,Integer>>> paramMiscProcessors;
    private String baseUrl;
    private Invoker invoker;

    public MethodHandler(Method method,boolean check,Invoker invoker) {
        if(check){
            boolean isFileResult = invoker.checkMethod(method);
            List<AnnotationRule> annotationRules = ProcessorFactory.getAnnotationRules();
            for(AnnotationRule rule:annotationRules){
                rule.checkMethod(method,isFileResult);
            }
        }
        this.invoker = invoker;
        Class<?> dc = method.getDeclaringClass();
        BaseURL baseURL = dc.getAnnotation(BaseURL.class);
        if(baseURL!=null){
            this.baseUrl = baseURL.value();
        }

        returnType = method.getGenericReturnType();
        Type[] methodParameterTypes = method.getGenericParameterTypes();
        methodParameterAnnotationArrays = method.getParameterAnnotations();
        methodAnnotations = method.getAnnotations();
        paramMiscProcessors = new HashMap<>();

        if(Util.hasUnresolvableType(returnType)){
            throw Util.methodError(method,
                    "Method return type must not include a type variable or wildcard: %s", returnType);
        }

        int count = methodParameterTypes.length;
        parameterProcessors = new ParameterProcessor[count][];
        int annotationCount;
        for(int i=0;i<count;i++){
            Type parameterType = methodParameterTypes[i];
            Annotation[] parameterAnnotations = methodParameterAnnotationArrays[i];
            annotationCount = parameterAnnotations.length;
            ParameterProcessor[] processors = new ParameterProcessor[annotationCount];
            for(int j=0;j<parameterAnnotations.length;j++){
                AbsParamProcessor processor  = ProcessorFactory.paramProcessor(parameterAnnotations[j]);
                if(check && processor!=null) processor.checkParameters(method,parameterAnnotations[j],parameterType);
                if(processor==null){
                    processors[j] = null;
                }else if(processor instanceof ParameterProcessor){
                    processors[j] = (ParameterProcessor) processor;
                }else{
                    processors[j] = null;
                    saveMiscProcessorAndPos((ParamMiscProcessor)processor,i,j);
                }
            }
            parameterProcessors[i] = processors;
        }
        int macount = methodAnnotations.length;
        methodProcessors = new MethodProcessor[macount];
        for(int i=0;i<macount;i++){
            methodProcessors[i] = ProcessorFactory.methodProcessor(methodAnnotations[i]);
        }
    }

    private void saveMiscProcessorAndPos(ParamMiscProcessor processor, int i, int j) {
        List<Pair<Integer,Integer>> list = paramMiscProcessors.get(processor);
        if(list==null){
            list = new ArrayList<>();
            paramMiscProcessors.put(processor,list);
        }
        list.add(new Pair<>(i,j));
    }

    private boolean checkReturnAndLastParameter(Method method, Type returnType, Type[] methodParameterTypes, boolean check) {
        Type lastParamType;
        if(check){
            if(methodParameterTypes.length==0){
                throw new IllegalArgumentException("the method define in the interface must have at least one paramter as Callback<T> or Clazz<T>");
            }
            lastParamType = methodParameterTypes[methodParameterTypes.length-1];
            if(Util.hasUnresolvableType(returnType)){
                throw Util.methodError(method,
                        "Method return type must not include a type variable or wildcard: %s", returnType);
            }
            if(Util.hasUnresolvableType(lastParamType)){
                throw Util.methodError(method,
                        "Method lastParamType must not include a type variable or wildcard: %s", returnType);
            }
        }else{
            lastParamType = methodParameterTypes[methodParameterTypes.length-1];
        }

        Type lastParamTypeRaw = Util.getRawType(lastParamType);
        boolean isSync = lastParamTypeRaw==Callback.class;

        if(check){
            if(Util.isSubType(lastParamType,Callback.class)){
                if(returnType != void.class && returnType != Handle.class){
                    throw Util.methodError(method, "the method define in the interface must return void or Handle/DownloadHandle");
                }
            }
            if(!isSync){
                Class[] exceptionClazzs = method.getExceptionTypes();
                if(exceptionClazzs.length!=1|| exceptionClazzs[0]!=Exception.class){
                    throw Util.methodError(method,"Sync method must declare throws Exception");
                }
            }
        }

        return isSync;
    }

    public Object invoke(Retrofit retrofit,RequestFilter filter,Object... args) throws Exception{
        Request request = retrofit.makeRequest(baseUrl);
        int maCount = methodProcessors.length;
        for(int i=0;i<maCount;i++){
            MethodProcessor processor = methodProcessors[i];
            if(processor!=null) processor.process(methodAnnotations[i],retrofit,request);
        }
        int length = args==null?0:args.length;
        for(int i=0;i<length;i++){
            ParameterProcessor[] processors = parameterProcessors[i];
            Annotation[] parameterAnnotations = methodParameterAnnotationArrays[i];
            for(int j=0;j<parameterAnnotations.length;j++){
                ParameterProcessor processor = processors[j];
                if(processor!=null) processor.process(parameterAnnotations[j],request,args[i]);
            }
        }
        if(!paramMiscProcessors.isEmpty()){
            for(ParamMiscProcessor processor:paramMiscProcessors.keySet()){
                processor.process(request,methodParameterAnnotationArrays,paramMiscProcessors.get(processor),args);
            }
        }
        Call call = retrofit.makeCall(request);
        call = new RetrofitCall(call,filter,retrofit);
        return invoker.invoke(call,returnType,args);
    }

//    @SuppressWarnings("unchecked")
//    private Object performReturn(Retrofit retrofit,RequestFilter filter,Request request, Type returnType,Object lastParam) throws Exception{
//
//        if(isCallbackMethod){
//            if(filter!=null) filter.onRequest(retrofit.lite(),request,Util.type(Callback.class, lastParam));
//            Object result = call.async((Callback) lastParam);
//            return (returnType == void.class) ? null:result;
//        }else{
//            if(filter!=null) filter.onRequest(retrofit.lite(),request,((Clazz)lastParam).type());
//            return call.sync((Clazz) lastParam);
//        }
//    }


}
