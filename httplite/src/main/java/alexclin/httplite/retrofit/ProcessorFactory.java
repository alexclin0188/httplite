package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.annotation.Body;
import alexclin.httplite.annotation.Cancel;
import alexclin.httplite.annotation.Form;
import alexclin.httplite.annotation.Forms;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.HTTP;
import alexclin.httplite.annotation.IntoFile;
import alexclin.httplite.annotation.JsonField;
import alexclin.httplite.annotation.Multipart;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.annotation.Progress;
import alexclin.httplite.annotation.Retry;
import alexclin.httplite.annotation.Tag;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.Util;

/**
 * ProcessorFactory
 *
 * @author alexclin
 * @date 16/1/28 19:20
 */
class ProcessorFactory {

    static List<MethodProcessor> methodProcessorList = new CopyOnWriteArrayList<>();
    static List<ParameterProcessor> paramterProcessorList = new CopyOnWriteArrayList<>();
    static List<AnnotationRule> annotationRuleList = new CopyOnWriteArrayList<>();
    static List<ParamMiscProcessor> paramMiscProcessors = new CopyOnWriteArrayList<>();

    static {
        methodProcessorList.add(new HttpMethodProcessor());
        annotationRuleList.add(new BasicRule());
        paramterProcessorList.add(new ListenerParamProcessor());
        paramterProcessorList.add(new BasicProcessors.BodyProcessor());
        paramterProcessorList.add(new BasicProcessors.FormProcessor());
        paramterProcessorList.add(new BasicProcessors.FormsProcessor());
        paramterProcessorList.add(new BasicProcessors.HeaderProcessor());
        paramterProcessorList.add(new BasicProcessors.HeadersProcessor());
        paramterProcessorList.add(new BasicProcessors.IntoFileProcessor());
        paramterProcessorList.add(new BasicProcessors.MultipartProcessor());
        paramterProcessorList.add(new BasicProcessors.ParamProcessor());
        paramterProcessorList.add(new BasicProcessors.ParamsProcessor());
        paramterProcessorList.add(new BasicProcessors.PathProcessor());
        paramterProcessorList.add(new BasicProcessors.PathsProcessor());
        paramMiscProcessors.add(new BasicProcessors.JsonFieldProcessor());
    }

    static List<AnnotationRule> getAnnotationRules() {
        return annotationRuleList;
    }

    static MethodProcessor methodProcessor(Annotation annotation) {
        for (MethodProcessor processor : methodProcessorList) {
            if (processor.support(annotation)) return processor;
        }
        throw new RuntimeException("unkown method annotation:" + annotation + ", to use custom annotaion, please set custom MethodProcessor in Retrofit");
    }

    static AbsParamProcessor paramProcessor(Annotation annotation) {
        for (ParameterProcessor processor : paramterProcessorList) {
            if (processor.support(annotation)) return processor;
        }
        for(ParamMiscProcessor processor : paramMiscProcessors){
            if (processor.support(annotation)) return processor;
        }
        throw new RuntimeException("unkown paramter annotation:" + annotation + ", to use custom annotaion, please set custom ParameterProcessor/ParamMiscProcessor in Retrofit");
    }

    private static boolean isBasicHttpAnnoation(Annotation annotation) {
        return (annotation instanceof GET) || (annotation instanceof POST) || (annotation instanceof HTTP);
    }

    private static HttpLite.Method getHttpAnnoationMethod(Annotation annotation) {
        if (annotation instanceof GET) {
            return HttpLite.Method.GET;
        } else if (annotation instanceof POST) {
            return HttpLite.Method.GET;
        } else if (annotation instanceof HTTP) {
            return ((HTTP) annotation).method();
        }
        return null;
    }

    /**
     * GET/POST/HTTP注解的处理
     */
    static class HttpMethodProcessor implements MethodProcessor {

        @Override
        public void process(Annotation annotation, Retrofit retrofit, Request request) {
            if (annotation instanceof GET) {
                retrofit.setMethod(request, HttpLite.Method.GET);
                retrofit.setUrl(request, ((GET) annotation).value());
            } else if (annotation instanceof POST) {
                retrofit.setMethod(request, HttpLite.Method.POST);
                retrofit.setUrl(request, ((POST) annotation).value());
            } else if (annotation instanceof HTTP) {
                retrofit.setMethod(request, ((HTTP) annotation).method());
                retrofit.setUrl(request, ((HTTP) annotation).path());
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return isBasicHttpAnnoation(annotation);
        }
    }

    /**
     * Progress/Retry/Cancel/Tag注解的处理
     */
    static class ListenerParamProcessor implements ParameterProcessor {

        @Override
        public void process(Annotation annotation, Request request, Object value) {
            if (annotation instanceof Progress) {
                request.onProgress((ProgressListener) value);
            } else if (annotation instanceof Retry) {
                request.onRetry((RetryListener) value);
            } else if (annotation instanceof Cancel) {
                request.onCancel((CancelListener) value);
            } else if (annotation instanceof Tag) {
                request.tag(value);
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return (annotation instanceof Progress) || (annotation instanceof Retry) ||
                    (annotation instanceof Cancel) || (annotation instanceof Tag);
        }

        @Override
        public void checkParameters(Method method,Annotation annotation, Type parameterType) throws RuntimeException {
            if ((annotation instanceof Progress) && !(Util.isSubType(parameterType, ProgressListener.class))) {
                throw Util.methodError(method, "The parameter with annotaion @Progress must implements ProgressListener");
            } else if ((annotation instanceof Retry) && !(Util.isSubType(parameterType, RetryListener.class))) {
                throw Util.methodError(method,"The parameter with annotaion @Retry must implements RetryListener");
            } else if ((annotation instanceof Cancel) && !(Util.isSubType(parameterType, CancelListener.class))) {
                throw Util.methodError(method,"The parameter with annotaion @Cancel must implements CancelListener");
            }
        }
    }

    static class BasicRule implements AnnotationRule {

        @Override
        public void checkMethod(Method checkMethod) throws RuntimeException {
            Type[] methodParameterTypes = checkMethod.getGenericParameterTypes();
            Annotation[][] methodParameterAnnotationArrays = checkMethod.getParameterAnnotations();
            Annotation[] methodAnnotations = checkMethod.getAnnotations();
            boolean hasHttpAnnotation = false;
            HttpLite.Method method = null;
            for (Annotation annotation : methodAnnotations) {
                boolean isBasicHttp = isBasicHttpAnnoation(annotation);
                method = getHttpAnnoationMethod(annotation);
                if (!isBasicHttp) continue;
                if (hasHttpAnnotation) {
                    String info = Util.printArray("You can use only one http annotation on each method but threre is:%s", methodAnnotations);
                    throw Util.methodError(checkMethod,info);
                } else {
                    hasHttpAnnotation = true;
                }
            }
            if (!hasHttpAnnotation) {
                String info = Util.printArray("You must set one http annotation on each method but threre is:%s", methodAnnotations);
                throw Util.methodError(checkMethod,info);
            }

            boolean isFileResturnOrCallback = Util.getTypeParameter(methodParameterTypes[methodParameterTypes.length-1])== File.class;
            boolean allowBody = Request.permitsRequestBody(method);
            boolean requireBody = Request.requiresRequestBody(method);
            boolean hasBodyAnnotation = false;
            boolean hasFormAnnotation = false;
            boolean hasMultipartAnnotation = false;
            boolean hasIntoFile = false;
            boolean hasJsonField = false;
            for (Annotation[] annotations : methodParameterAnnotationArrays) {
                for (Annotation annotation : annotations) {
                    if (annotation instanceof Body) {
                        if (hasBodyAnnotation) {
                            throw Util.methodError(checkMethod, "You can use @Body annoation on method not more than once");
                        }else if (!allowBody) {
                            throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                        }
                        hasBodyAnnotation = true;
                    } else if ((annotation instanceof Form) || (annotation instanceof Forms)) {
                        if (!allowBody) {
                            throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                        }
                        hasFormAnnotation = true;
                    } else if ((annotation instanceof Multipart)) {
                        if (!allowBody) {
                            throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                        }
                        hasMultipartAnnotation = true;
                    } else if (annotation instanceof IntoFile) {
                        if (!isFileResturnOrCallback)
                            throw Util.methodError(checkMethod,"Use @InfoFile must with last parameter (Callback<File>) or (Clazz<File> and return file)");
                        hasIntoFile = true;
                    } else if (annotation instanceof JsonField){
                        if (!allowBody) {
                            throw Util.methodError(checkMethod,String.format("HttpMethod:%s don't allow body param", method));
                        }
                        hasJsonField = true;
                    }
                    if (hasFormAnnotation&&hasBodyAnnotation) {
                        throw Util.methodError(checkMethod,"You can not use @Body and @Form/Forms on on the same one method");
                    } else if (hasMultipartAnnotation&&hasBodyAnnotation) {
                        throw Util.methodError(checkMethod,"You can not use @Body and @Multipart on on the same one method");
                    } else if(hasJsonField&&hasBodyAnnotation){
                        throw Util.methodError(checkMethod,"You can not use @Body and @JsonField on on the same one method");
                    } else if(hasFormAnnotation&&hasMultipartAnnotation){
                        throw Util.methodError(checkMethod,"You can not use @Form/Forms and @Multipart on on the same one method");
                    } else if(hasFormAnnotation&&hasJsonField){
                        throw Util.methodError(checkMethod,"You can not use @JsonFile and @Multipart on on the same one method");
                    } else if(hasMultipartAnnotation&&hasJsonField){
                        throw Util.methodError(checkMethod,"You can not use @Form/Forms and @JsonField on on the same one method");
                    }
                }
            }

            if(isFileResturnOrCallback&&!hasIntoFile){
                throw Util.methodError(checkMethod,"Use last parameter (Callback<File>) or (Clazz<File> and return file), method must has @IntoFile paramter");
            }

            if(requireBody && !(hasBodyAnnotation||hasFormAnnotation||hasMultipartAnnotation||hasJsonField)){
                throw Util.methodError(checkMethod,"Http method:%s must has body parameter such as:@Form/Forms @Multipart @Body @JsonField",method);
            }
        }
    }

    abstract static class ObjectsProcessor implements ParameterProcessor{
        @Override
        public void process(Annotation annotation, Request request, Object value) {
            Class clazz = value.getClass();
            if(Util.isSubType(clazz, Collection.class)){
                Collection collection = (Collection) value;
                for(Object obj:collection){
                    if(obj==null) return;
                    performProcess(annotation,request,obj);
                }
            }else if(clazz.getGenericSuperclass() instanceof GenericArrayType){
                Object[] objects = (Object[]) value;
                for(Object obj:objects){
                    performProcess(annotation,request,obj);
                }
            }else {
                performProcess(annotation, request, value);
            }
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {}

        abstract void performProcess(Annotation annotation, Request request, Object value);
    }

    abstract static class MapProcessor implements ParameterProcessor{

        @Override @SuppressWarnings("unchecked")
        public void process(Annotation annotation, Request request, Object value) {
            if(value==null) return;
            Map<String,?> map = (Map<String,?>) value;
            for(String key:map.keySet()){
                Object object = map.get(key);
                if(key==null|| object==null) return;
                performProcess(annotation, request,key, object);
            }
        }

        abstract void performProcess(Annotation annotation, Request request,String key, Object value);

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            //必须为Map<String,?>
            boolean isMapStr = false;
            if(parameterType instanceof ParameterizedType){
                if(Util.isSubType(parameterType, Map.class)){
                    Type[] typeParams = ((ParameterizedType)parameterType).getActualTypeArguments();
                    if(typeParams.length>0&&typeParams[0]==String.class){
                        isMapStr = true;
                    }
                }
            }
            if(!isMapStr){
                throw Util.methodError(method,"Annotation @%s must use for parameter type Map<String,?>",annotation);
            }
        }
    }
}
