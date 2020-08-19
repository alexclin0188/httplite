package alexclin.httplite.retrofit;

import android.util.Pair;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.annotation.BaseURL;
import alexclin.httplite.util.Util;

/**
 * MethodHandler
 *
 * @author alexclin 16/1/28 19:20
 */
class MethodHandler implements CallAdapter.RequestCreator{
    private ParameterProcessor[][] parameterProcessors;
    private Type returnType;
    private Annotation[][] methodParameterAnnotationArrays;
    private Map<ParamMiscProcessor,List<Pair<Integer,Integer>>> paramMiscProcessors;
    private CallAdapter invoker;
    private Request.Builder originBuilder;
    private final String mBaseUrl;
    private final Retrofit mRetrofit;

    MethodHandler(Method method,Retrofit retrofit,CallAdapter invoker) {
        mRetrofit = retrofit;
        if(!retrofit.isReleaseMode){
            CallAdapter.ResultType rt = invoker.checkMethod(method);
            List<AnnotationRule> annotationRules = ProcessorFactory.getAnnotationRules();
            for(AnnotationRule rule:annotationRules){
                rule.checkMethod(method,rt);
            }
        }
        this.invoker = invoker;
        Class<?> dc = method.getDeclaringClass();
        BaseURL baseURL = dc.getAnnotation(BaseURL.class);
        mBaseUrl = baseURL!=null?baseURL.value():null;
        this.originBuilder = new Request.Builder();

        Annotation[] methodAnnotations = method.getAnnotations();
        int methodAnnoCount = methodAnnotations.length;
        MethodProcessor[] methodProcessors = new MethodProcessor[methodAnnoCount];
        for(int i=0;i<methodAnnoCount;i++){
            methodProcessors[i] = ProcessorFactory.methodProcessor(methodAnnotations[i]);
        }
        int maCount = methodProcessors.length;
        for(int i=0;i<maCount;i++){
            MethodProcessor processor = methodProcessors[i];
            if(processor!=null) processor.process(method,methodAnnotations[i],retrofit, originBuilder);
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
                if(!retrofit.isReleaseMode && processor!=null) processor.checkParameters(method,parameterAnnotations[j],parameterType);
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
        if(!paramMiscProcessors.isEmpty()&&!retrofit.isReleaseMode){
            for(ParamMiscProcessor processor:paramMiscProcessors.keySet()){
                processor.checkMiscParameters(method,methodParameterAnnotationArrays,paramMiscProcessors.get(processor),methodParameterTypes);
            }
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

    Object invoke(Object[] args) throws Exception{
        return invoker.adapt(mRetrofit.lite,this,returnType,args);
    }

    @Override
    public Request createRequest(Object[] args) {
        try {
            Request.Builder builder = (Request.Builder) originBuilder.clone();
            int length = args==null?0:args.length;
            for(int i=0;i<length;i++){
                ParameterProcessor[] processors = parameterProcessors[i];
                Annotation[] parameterAnnotations = methodParameterAnnotationArrays[i];
                Object arg = args[i];
                for(int j=0;j<parameterAnnotations.length;j++){
                    ParameterProcessor processor = processors[j];
                    if(processor!=null) processor.process(parameterAnnotations[j],builder,arg);
                }
            }
            if(!paramMiscProcessors.isEmpty()){
                for(ParamMiscProcessor processor:paramMiscProcessors.keySet()){
                    processor.process(builder,methodParameterAnnotationArrays,paramMiscProcessors.get(processor),args);
                }
            }
            builder.baseUrl(mBaseUrl);
            return builder.build();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }
}
