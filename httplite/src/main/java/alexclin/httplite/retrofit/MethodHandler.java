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
import alexclin.httplite.Request;
import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.util.Util;

/**
 * MethodHandler
 *
 * @author alexclin 16/1/28 19:20
 */
public class MethodHandler<T> {
    private ParameterProcessor[][] parameterProcessors;
    private Type returnType;
    private Annotation[][] methodParameterAnnotationArrays;
    private Map<ParamMiscProcessor,List<Pair<Integer,Integer>>> paramMiscProcessors;
    private CallAdapter invoker;
    private Request originRequest;

    public MethodHandler(Method method,Retrofit retrofit,CallAdapter invoker) {
        if(!retrofit.isReleaseMode()){
            boolean isFileResult = invoker.checkMethod(method);
            List<AnnotationRule> annotationRules = ProcessorFactory.getAnnotationRules();
            for(AnnotationRule rule:annotationRules){
                rule.checkMethod(method,isFileResult);
            }
        }
        this.invoker = invoker;
        Class<?> dc = method.getDeclaringClass();
        BaseURL baseURL = dc.getAnnotation(BaseURL.class);
        this.originRequest = retrofit.makeRequest(baseURL!=null?baseURL.value():null);

        Annotation[] methodAnnotations = method.getAnnotations();
        int methodAnnoCount = methodAnnotations.length;
        MethodProcessor[] methodProcessors = new MethodProcessor[methodAnnoCount];
        for(int i=0;i<methodAnnoCount;i++){
            methodProcessors[i] = ProcessorFactory.methodProcessor(methodAnnotations[i]);
        }
        int maCount = methodProcessors.length;
        for(int i=0;i<maCount;i++){
            MethodProcessor processor = methodProcessors[i];
            if(processor!=null) processor.process(method,methodAnnotations[i],retrofit,originRequest);
        }

        returnType = method.getGenericReturnType();
        Type[] methodParameterTypes = method.getGenericParameterTypes();
        methodParameterAnnotationArrays = method.getParameterAnnotations();
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
                if(!retrofit.isReleaseMode() && processor!=null) processor.checkParameters(method,parameterAnnotations[j],parameterType);
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
    }

    private void saveMiscProcessorAndPos(ParamMiscProcessor processor, int i, int j) {
        List<Pair<Integer,Integer>> list = paramMiscProcessors.get(processor);
        if(list==null){
            list = new ArrayList<>();
            paramMiscProcessors.put(processor,list);
        }
        list.add(new Pair<>(i,j));
    }

    public Object invoke(Retrofit retrofit, RequestListener filter, Object... args) throws Exception{
        Request request = originRequest.clone();
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
        return invoker.adapt(call,returnType,args);
    }
}
