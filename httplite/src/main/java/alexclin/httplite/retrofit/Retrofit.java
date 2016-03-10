package alexclin.httplite.retrofit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.util.Util;

/**
 * Retrofit
 *
 * @author alexclin
 * @date 16/1/5 23:06
 */
public abstract class Retrofit {

    private final Map<Method,MethodHandler> methodHandlerCache = new LinkedHashMap<>();  //TODO LinkedHashMap合不合适？

    private MethodListener methodListener;

    @SuppressWarnings("unchecked")
    public final <T> T create(final Class<T> service,final RequestFilter filter){
        Util.validateServiceInterface(service);
        if (!isReleaseMode()) {
            eagerlyValidateMethods(service);
        }
        return (T) Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[]{service},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object... args)
                            throws Throwable {
                        // If the method is a method from Object then defer to normal invocation.
                        if (method.getDeclaringClass() == Object.class) {
                            return method.invoke(this, args);
                        }
                        if (isDefaultMethod(method)) {
                            return invokeDefaultMethod(method, service, proxy, args);
                        }
                        dispatchMethodEvent(method,Retrofit.this,args);
                        return loadMethodHandler(method).invoke(Retrofit.this,filter,args);
                    }
                });
    }

    private <T> void eagerlyValidateMethods(Class<T> service) {
        for(Method method:service.getDeclaredMethods()){
            if(!isDefaultMethod(method))
                loadMethodHandler(method);
        }
    }

    protected void dispatchMethodEvent(Method method, Retrofit retrofit, Object... args){
        if(methodListener!=null){
            methodListener.onMethod(method,retrofit,args);
        }
    }

    public void setMethodListener(MethodListener methodListener){
        this.methodListener = methodListener;
    }

    private static boolean isDefaultMethod(Method method){
        //TODO
        return false;
    }

    public static <T> Object invokeDefaultMethod(Method method,Class<T> service,Object proxy,Object... args){
        //TODO
        return null;
    }

    MethodHandler loadMethodHandler(Method method) {
        MethodHandler handler;
        synchronized (methodHandlerCache) {
            handler = methodHandlerCache.get(method);
            if (handler == null) {
                handler = new MethodHandler(method,!isReleaseMode());
                methodHandlerCache.put(method, handler);
            }
        }
        return handler;
    }

    public abstract Request makeRequest();

    public abstract Request setMethod(Request request,alexclin.httplite.Method method);

    public abstract Request setUrl(Request request, String url);

    public abstract Call makeCall(Request request);

    public abstract boolean isReleaseMode();

    public abstract HttpLite lite();

    public static void registerParamterProcessor(ParameterProcessor processor) {
        if (processor != null && !ProcessorFactory.paramterProcessorList.contains(processor)) {
            ProcessorFactory.paramterProcessorList.add(processor);
        }
    }

    public static void unregisterParamterProcessor(ParameterProcessor processor) {
        if (processor != null) {
            ProcessorFactory.paramterProcessorList.remove(processor);
        }
    }

    public static void unregisterParamterProcessor(Class<? extends ParameterProcessor> clazz) {
        Iterator<ParameterProcessor> iterator = ProcessorFactory.paramterProcessorList.iterator();
        while (iterator.hasNext()){
            ParameterProcessor processor = iterator.next();
            if(processor.getClass()==clazz){
                iterator.remove();
            }
        }
    }

    public static void registerParamMiscProcessor(ParamMiscProcessor processor) {
        if (processor != null && !ProcessorFactory.paramMiscProcessors.contains(processor)) {
            ProcessorFactory.paramMiscProcessors.add(processor);
        }
    }

    public static void unregisterParamMiscProcessor(ParamMiscProcessor processor) {
        if (processor != null) {
            ProcessorFactory.paramMiscProcessors.remove(processor);
        }
    }

    public static void unregisterParamMiscProcessor(Class<? extends ParamMiscProcessor> clazz) {
        Iterator<ParamMiscProcessor> iterator = ProcessorFactory.paramMiscProcessors.iterator();
        while (iterator.hasNext()){
            ParamMiscProcessor processor = iterator.next();
            if(processor.getClass()==clazz){
                iterator.remove();
            }
        }
    }

    public static void registerMethodProcessor(MethodProcessor processor) {
        if (processor != null && !ProcessorFactory.methodProcessorList.contains(processor)) {
            ProcessorFactory.methodProcessorList.add(processor);
        }
    }

    public static void unregisterMethodProcessor(MethodProcessor processor) {
        if (processor != null) {
            ProcessorFactory.methodProcessorList.remove(processor);
        }
    }

    public static void unregisterMethodProcessor(Class<? extends MethodProcessor> clazz) {
        Iterator<MethodProcessor> iterator = ProcessorFactory.methodProcessorList.iterator();
        while (iterator.hasNext()){
            MethodProcessor processor = iterator.next();
            if(processor.getClass()==clazz){
                iterator.remove();
            }
        }
    }

    public static void registerAnnotationRule(AnnotationRule rule) {
        if (rule != null && !ProcessorFactory.annotationRuleList.contains(rule)) {
            ProcessorFactory.annotationRuleList.add(rule);
        }
    }

    public static void unregisterAnnotationRule(AnnotationRule rule) {
        if (rule != null) {
            ProcessorFactory.annotationRuleList.remove(rule);
        }
    }

    public static void unregisterAnnotationRule(Class<? extends AnnotationRule> clazz) {
        Iterator<AnnotationRule> iterator = ProcessorFactory.annotationRuleList.iterator();
        while (iterator.hasNext()){
            AnnotationRule processor = iterator.next();
            if(processor.getClass()==clazz){
                iterator.remove();
            }
        }
    }
}
