package alexclin.httplite.retrofit;

import android.os.Build;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.util.HttpMethod;
import alexclin.httplite.util.Util;

/**
 * Retrofit
 *
 * @author alexclin 16/1/5 23:06
 */
public abstract class Retrofit {

    private final Map<Method,MethodHandler> methodHandlerCache = new LinkedHashMap<>();  //TODO LinkedHashMap？

    private List<CallAdapter> mInvokers;

    public Retrofit(List<CallAdapter> invokers) {
        mInvokers = new ArrayList<>();
        if(invokers!=null){
            mInvokers.addAll(invokers);
        }
        mInvokers.addAll(BasicCallAdapters.basicAdapters());
    }

    private static boolean isDefaultMethod(Method method){
        if(Build.VERSION.SDK_INT>23){
            //TODO java8
        }
        return false;
    }

    public static <T> Object invokeDefaultMethod(Method method,Class<T> service,Object proxy,Object... args){
        if(Build.VERSION.SDK_INT>23){
            //TODO java8
        }
        return null;
    }

    public static void registerParamterProcessor(ParameterProcessor processor) {
        if (processor != null && !ProcessorFactory.parameterProcessorList.contains(processor)) {
            ProcessorFactory.parameterProcessorList.add(processor);
        }
    }

    public static void registerParamMiscProcessor(ParamMiscProcessor processor) {
        if (processor != null && !ProcessorFactory.paramMiscProcessors.contains(processor)) {
            ProcessorFactory.paramMiscProcessors.add(processor);
        }
    }

    public static void registerMethodProcessor(MethodProcessor processor) {
        if (processor != null && !ProcessorFactory.methodProcessorList.contains(processor)) {
            ProcessorFactory.methodProcessorList.add(processor);
        }
    }

    public static void registerAnnotationRule(AnnotationRule rule) {
        if (rule != null && !ProcessorFactory.annotationRuleList.contains(rule)) {
            ProcessorFactory.annotationRuleList.add(rule);
        }
    }

    public static void registerBodyAnnotation(Class<? extends Annotation> clazz, String type, boolean allowRepeat){
        ProcessorFactory.basicAnnotationRule.registerBodyAnnotation(clazz,type,allowRepeat);
    }

    public static void ignoreAnnotation(Class<? extends Annotation> annotation){
        ProcessorFactory.addIgnoreAnnotation(annotation);
    }

    @SuppressWarnings("unchecked")
    public final <T> T create(Class<T> service, RequestListener filter, MethodFilter methodFilter){
        Util.validateServiceInterface(service);
        if (!isReleaseMode()) {
            eagerlyValidateMethods(service);
        }
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new ProxyInvoker<T>(service,filter,methodFilter));
    }

    private <T> void eagerlyValidateMethods(Class<T> service) {
        for(Method method:service.getDeclaredMethods()){
            if(!isDefaultMethod(method))
                loadMethodHandler(method);
        }
    }

    MethodHandler loadMethodHandler(Method method) {
        MethodHandler handler;
        synchronized (methodHandlerCache) {
            handler = methodHandlerCache.get(method);
            if (handler == null) {
                handler = new MethodHandler(method,this,searchInvoker(method));
                methodHandlerCache.put(method, handler);
            }
        }
        return handler;
    }

    public abstract Request makeRequest(String baseUrl);

    public abstract Request setMethod(Request request, HttpMethod method);

    public abstract Request setUrl(Request request, String url);

    public abstract Call makeCall(Request request);

    public abstract boolean isReleaseMode();

    public abstract HttpLite lite();

    private CallAdapter searchInvoker(Method method) throws RuntimeException{
        for(CallAdapter invoker:mInvokers){
            if(invoker.support(method)){
                return invoker;
            }
        }
        throw Util.methodError(method,"no CallAdapter for %s",method.getName());
    }

    private class ProxyInvoker<T> implements InvocationHandler{
        private final Class<T> service;
        private final RequestListener filter;
        private final MethodFilter methodFilter;
        private final LinkedHashMap<Method,MethodInvoker> invokerMap;

        public ProxyInvoker(Class<T> service, RequestListener filter, MethodFilter methodFilter) {
            this.service = service;
            this.filter = filter;
            this.methodFilter = methodFilter;
            if(this.methodFilter!=null){
                this.invokerMap = new LinkedHashMap<>();
            }else {
                this.invokerMap = null;
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // If the method is a method from Object then defer to normal invocation.
            if (method.getDeclaringClass() == Object.class) {
                return method.invoke(this, args);
            }
            if (isDefaultMethod(method)) {
                return invokeDefaultMethod(method, service, proxy, args);
            }
            MethodHandler<?> handler = loadMethodHandler(method);
            if(invokerMap==null){
                return handler.invoke(Retrofit.this,filter,args);
            }else{
                MethodInvoker methodInvoker;
                synchronized (invokerMap){
                    methodInvoker = invokerMap.get(method);
                    if(methodInvoker==null){
                        methodInvoker = new MethodInvoker(handler,method,Retrofit.this,filter);
                        invokerMap.put(method,methodInvoker);
                    }
                }
                return methodFilter.onMethod(lite(),methodInvoker,args);
            }
        }
    }
}
