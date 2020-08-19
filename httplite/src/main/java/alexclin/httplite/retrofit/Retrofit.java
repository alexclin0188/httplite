package alexclin.httplite.retrofit;

import android.os.Build;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import alexclin.httplite.HttpLite;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * Retrofit
 *
 * @author alexclin 16/1/5 23:06
 */
public class Retrofit {

    private final Map<Method,MethodHandler> methodHandlerCache = new LinkedHashMap<>();  //TODO LinkedHashMap？

    private List<CallAdapter> mInvokers;
    final boolean isReleaseMode;
    final HttpLite lite;

    public Retrofit(HttpLite lite,boolean release) {
        this(lite,null,null,release);
    }

    public Retrofit(HttpLite lite, Collection<CallAdapter> invokers, boolean release) {
        this(lite,invokers,null,release);
    }

    public Retrofit(HttpLite lite,ExecutorService executor,boolean release) {
        this(lite,null,executor,release);
    }

    public Retrofit(HttpLite lite,Collection<CallAdapter> invokers,ExecutorService executor,boolean release) {
        mInvokers = new ArrayList<>();
        if(invokers!=null){
            mInvokers.addAll(invokers);
        }
        mInvokers.addAll(BasicCallAdapters.basicAdapters(executor));
        isReleaseMode = release;
        this.lite = lite;
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
    public final <T> T create(final Class<T> service){
        Util.validateServiceInterface(service);
        if (!isReleaseMode) {
            eagerlyValidateMethods(service);
        }
        InvocationHandler invocationHandler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // If the method is a method from Object then defer to normal invocation.
                if (method.getDeclaringClass() == Object.class) {
                    return method.invoke(this, args);
                }
                if (isDefaultMethod(method)) {
                    return invokeDefaultMethod(method, service, proxy, args);
                }
                MethodHandler handler = loadMethodHandler(method);
                return handler.invoke(args);
            }
        };
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                invocationHandler);
    }

    private <T> void eagerlyValidateMethods(Class<T> service) {
        for(Method method:service.getDeclaredMethods()){
            if(!isDefaultMethod(method))
                loadMethodHandler(method);
        }
    }

    private MethodHandler loadMethodHandler(Method method) {
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

    private CallAdapter searchInvoker(Method method) throws RuntimeException{
        for(CallAdapter invoker:mInvokers){
            LogUtil.e("invoker:"+invoker.getClass().getSimpleName()+",support:"+invoker.support(method));
            if(invoker.support(method)){
                return invoker;
            }
        }
        throw Util.methodError(method,"No CallAdapter for %s",method.getName());
    }
}
