package alexclin.httplite.retrofit;

import android.util.Pair;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Util;

/**
 * MethodHandler
 *
 * @author alexclin
 * @date 16/1/28 19:20
 */
public class MethodHandler {
    private MethodProcessor[] methodProcessors;
    private ParameterProcessor[][] paramterProcessors;
    private Type returnType;
    private Annotation[][] methodParameterAnnotationArrays;
    private Annotation[] methodAnnotations;
    private Map<ParamMiscProcessor,List<Pair<Integer,Integer>>> paramMiscProcessors;
    private boolean isSyncMethod;

    public MethodHandler(Method method,boolean check) {
        if(check){
            List<AnnotationRule> annotationRules = ProcessorFactory.getAnnotationRules();
            for(AnnotationRule rule:annotationRules){
                rule.checkMethod(method);
            }
        }

        returnType = method.getGenericReturnType();
        Type[] methodParameterTypes = method.getGenericParameterTypes();
        isSyncMethod = checkReturnAndLastParameter(method, returnType, methodParameterTypes, check);
        methodParameterAnnotationArrays = method.getParameterAnnotations();
        methodAnnotations = method.getAnnotations();
        paramMiscProcessors = new HashMap<>();

        int count = methodParameterTypes.length;
        paramterProcessors = new ParameterProcessor[count][];
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
            paramterProcessors[i] = processors;
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
        boolean isSync = lastParamTypeRaw==Clazz.class;

        if(check){
            Type typeParam = Util.getTypeParameter(lastParamType);
            if(lastParamTypeRaw==Clazz.class){
                if(!returnType.equals(typeParam)){
                    throw Util.methodError(method, "the return type must be same as the type T in Clazz<T> when you use Clazz<T> as last param type");
                }
            }else if(Util.isSubType(lastParamType,Callback.class)){
                if(returnType != void.class && returnType != DownloadHandle.class && returnType != Handle.class){
                    throw Util.methodError(method, "the method define in the interface must return void or Handle/DownloadHandle");
                }
                if(typeParam!=File.class && returnType == DownloadHandle.class){
                    throw Util.methodError(method, "the interface method return DownloadHandle must use type Callback<File> as last param type");
                }
            }else {
                throw Util.methodError(method,
                        "Method lastParamType must be Callback<T> or Clazz<T> but is: %s", lastParamType);
            }
            if(isSync){
                Class[] exceptionClazzs = method.getExceptionTypes();
                if(exceptionClazzs.length!=1|| exceptionClazzs[0]!=Exception.class){
                    throw Util.methodError(method,"Sync method must declare throws Exception");
                }
            }
        }

        return isSync;
    }

    public Object invoke(Retrofit retrofit,Object... args) throws Exception{
        Request request = retrofit.makeRequest();
        int maCount = methodProcessors.length;
        for(int i=0;i<maCount;i++){
            MethodProcessor processor = methodProcessors[i];
            if(processor!=null) processor.process(methodAnnotations[i],retrofit,request);
        }
        int length = args.length;
        for(int i=0;i<length;i++){
            ParameterProcessor[] processors = paramterProcessors[i];
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
        return performReturn(retrofit,request,returnType,args[args.length-1]);
    }

    @SuppressWarnings("unchecked")
    private Object performReturn(Retrofit retrofit,Request request, Type returnType,Object lastParam) throws Exception{
        Call call = retrofit.makeCall(request);
        if(isSyncMethod){
            return call.executeSync((Clazz)lastParam);
        }else{
            if(returnType==DownloadHandle.class){
                return call.download((Callback<File>)lastParam);
            }else{
                Object result = call.execute((Callback) lastParam);
                return (returnType==Handle.class)?result:null;
            }
        }
    }
}
