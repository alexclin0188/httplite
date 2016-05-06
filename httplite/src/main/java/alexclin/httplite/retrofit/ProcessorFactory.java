package alexclin.httplite.retrofit;

import android.text.TextUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.annotation.FixHeaders;
import alexclin.httplite.annotation.GET;
import alexclin.httplite.annotation.HTTP;
import alexclin.httplite.annotation.Mark;
import alexclin.httplite.annotation.POST;
import alexclin.httplite.annotation.Progress;
import alexclin.httplite.annotation.Retry;
import alexclin.httplite.annotation.Tag;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.HttpMethod;
import alexclin.httplite.util.Util;

/**
 * ProcessorFactory
 *
 * @author alexclin 16/1/28 19:20
 */
class ProcessorFactory {

    static List<MethodProcessor> methodProcessorList = new CopyOnWriteArrayList<>();
    static List<ParameterProcessor> parameterProcessorList = new CopyOnWriteArrayList<>();
    static List<AnnotationRule> annotationRuleList = new CopyOnWriteArrayList<>();
    static List<ParamMiscProcessor> paramMiscProcessors = new CopyOnWriteArrayList<>();
    static List<Class<? extends Annotation>> ignoreAnnotations = new CopyOnWriteArrayList<>();
    static BasicAnnotationRule basicAnnotationRule = new BasicAnnotationRule();

    static {
        annotationRuleList.add(basicAnnotationRule);
        methodProcessorList.add(new HttpMethodProcessor());
        methodProcessorList.add(new MarkAndHeadersProcessor());
        parameterProcessorList.add(new ListenerParamProcessor());
        parameterProcessorList.add(new BasicProcessors.BodyProcessor());
        parameterProcessorList.add(new BasicProcessors.FormProcessor());
        parameterProcessorList.add(new BasicProcessors.FormsProcessor());
        parameterProcessorList.add(new BasicProcessors.HeaderProcessor());
        parameterProcessorList.add(new BasicProcessors.HeadersProcessor());
        parameterProcessorList.add(new BasicProcessors.IntoFileProcessor());
        parameterProcessorList.add(new BasicProcessors.MultipartProcessor());
        parameterProcessorList.add(new BasicProcessors.ParamProcessor());
        parameterProcessorList.add(new BasicProcessors.ParamsProcessor());
        parameterProcessorList.add(new BasicProcessors.PathProcessor());
        parameterProcessorList.add(new BasicProcessors.PathsProcessor());
        paramMiscProcessors.add(new BasicProcessors.JsonFieldProcessor());
        addIgnoreAnnotation(FixHeaders.class);
    }

    static List<AnnotationRule> getAnnotationRules() {
        return annotationRuleList;
    }

    static MethodProcessor methodProcessor(Annotation annotation) {
        if (isSystemAnnotation(annotation) || isIgnoreAnnotation(annotation.getClass())) return null;
        for (MethodProcessor processor : methodProcessorList) {
            if (processor.support(annotation)) return processor;
        }
        throw new RuntimeException("unknown method annotation:" + annotation + ", to use custom annotation, please set custom MethodProcessor in Retrofit");
    }

    static AbsParamProcessor paramProcessor(Annotation annotation) {
        if (isSystemAnnotation(annotation) || isIgnoreAnnotation(annotation.getClass())) return null;
        for (ParameterProcessor processor : parameterProcessorList) {
            if (processor.support(annotation)) return processor;
        }
        for (ParamMiscProcessor processor : paramMiscProcessors) {
            if (processor.support(annotation)) return processor;
        }
        throw new RuntimeException("unknown parameter annotation:" + annotation + ", to use custom annotation, please set custom ParameterProcessor/ParamMiscProcessor in Retrofit");
    }

    public static boolean isBasicHttpAnnotation(Annotation annotation) {
        return (annotation instanceof GET) || (annotation instanceof POST) || (annotation instanceof HTTP);
    }


    public static boolean isSystemAnnotation(Annotation annotation) {
        String packageName = annotation.getClass().getName();
        return packageName.startsWith("java.lang.annotation") || packageName.startsWith("android.support.") || packageName.startsWith("android.annotation.");
    }

    public static void addIgnoreAnnotation(Class<? extends Annotation> annotation) {
        ignoreAnnotations.add(annotation);
    }

    public static boolean isIgnoreAnnotation(Class<? extends Annotation> clazz) {
        return ignoreAnnotations.contains(clazz);
    }

    /**
     * GET/POST/HTTP注解的处理
     */
    static class HttpMethodProcessor implements MethodProcessor {

        @Override
        public void process(Method method,Annotation annotation, Retrofit retrofit, Request request) {
            if (annotation instanceof GET) {
                retrofit.setMethod(request, HttpMethod.GET);
                retrofit.setUrl(request, ((GET) annotation).value());
            } else if (annotation instanceof POST) {
                retrofit.setMethod(request, HttpMethod.POST);
                retrofit.setUrl(request, ((POST) annotation).value());
            } else if (annotation instanceof HTTP) {
                retrofit.setMethod(request, ((HTTP) annotation).method());
                retrofit.setUrl(request, ((HTTP) annotation).path());
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return isBasicHttpAnnotation(annotation);
        }
    }

    static class MarkAndHeadersProcessor implements MethodProcessor {

        @Override
        public void process(Method method,Annotation annotation, Retrofit retrofit, Request request) {
            if (annotation instanceof Mark)
                request.mark(((Mark) annotation).value());
            else if(annotation instanceof FixHeaders){
                String[] headers = ((FixHeaders) annotation).value();
                for (String header : headers) {
                    int colon = header.indexOf(':');
                    if (colon == -1 || colon == 0 || colon == header.length() - 1) {
                        throw Util.methodError(method,
                                "@Headers value must be in the form \"Name: Value\". Found: \"%s\"", header);
                    }
                    String headerName = header.substring(0, colon);
                    String headerValue = header.substring(colon + 1).trim();
                    request.addHeader(headerName, headerValue);
                }
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return (annotation instanceof Mark) || (annotation instanceof FixHeaders);
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
            } else if (annotation instanceof Tag) {
                request.tag(value);
            }
        }

        @Override
        public boolean support(Annotation annotation) {
            return (annotation instanceof Progress) || (annotation instanceof Retry) || (annotation instanceof Tag);
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            if ((annotation instanceof Progress) && !(Util.isSubType(parameterType, ProgressListener.class))) {
                throw Util.methodError(method, "The parameter with annotation @Progress must implements ProgressListener");
            } else if ((annotation instanceof Retry) && !(Util.isSubType(parameterType, RetryListener.class))) {
                throw Util.methodError(method, "The parameter with annotation @Retry must implements RetryListener");
            }
        }
    }

    abstract static class ObjectsProcessor implements ParameterProcessor {
        @Override
        public void process(Annotation annotation, Request request, Object value) {
            if (value == null) return;
            Class clazz = value.getClass();
            if (Util.isSubType(clazz, Collection.class)) {
                Collection collection = (Collection) value;
                for (Object obj : collection) {
                    if (obj == null) return;
                    performProcess(annotation, request, obj);
                }
            } else if (clazz.getGenericSuperclass() instanceof GenericArrayType) {
                Object[] objects = (Object[]) value;
                for (Object obj : objects) {
                    if (obj == null) return;
                    performProcess(annotation, request, obj);
                }
            } else {
                performProcess(annotation, request, value);
            }
        }

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            if (TextUtils.isEmpty(value(annotation))) {
                throw Util.methodError(method, "The annotation {@%s(value) value} must not be null", annotation);
            }
        }

        abstract void performProcess(Annotation annotation, Request request, Object value);

        abstract String value(Annotation annotation);
    }

    abstract static class MapProcessor implements ParameterProcessor {

        @Override
        @SuppressWarnings("unchecked")
        public void process(Annotation annotation, Request request, Object value) {
            if (value == null) return;
            Map<String, ?> map = (Map<String, ?>) value;
            for (String key : map.keySet()) {
                Object object = map.get(key);
                if (TextUtils.isEmpty(key) || object == null) return;
                performProcess(annotation, request, key, object);
            }
        }

        abstract void performProcess(Annotation annotation, Request request, String key, Object value);

        @Override
        public void checkParameters(Method method, Annotation annotation, Type parameterType) throws RuntimeException {
            //必须为Map<String,?>
            boolean isMapStr = false;
            if (parameterType instanceof ParameterizedType) {
                if (Util.isSubType(parameterType, Map.class)) {
                    Type[] typeParams = ((ParameterizedType) parameterType).getActualTypeArguments();
                    if (typeParams.length > 0 && typeParams[0] == String.class) {
                        isMapStr = true;
                    }
                }
            }
            if (!isMapStr) {
                throw Util.methodError(method, "Annotation @%s must use for parameter type Map<String,?>", annotation);
            }
        }
    }
}
