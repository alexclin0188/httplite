package alexclin.httplite.retrofit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import alexclin.httplite.Call;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.util.Util;

/**
 * Retrofit
 *
 * @author alexclin
 * @date 16/1/5 23:06
 */
public abstract class Retrofit {

    private final Map<Method,MethodHandler> methodHandlerCache = new LinkedHashMap<>();

    private boolean isReleaseMode = false;

    private final List<MethodListener> methodListenerList = new CopyOnWriteArrayList<>();

    @SuppressWarnings("unchecked")
    public final <T> T create(final Class<T> service){
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
                        return loadMethodHandler(method).invoke(Retrofit.this,args);
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
        if(!methodListenerList.isEmpty()){
            for(MethodListener listener:methodListenerList){
                listener.onMethod(method,retrofit,args);
            }
        }
    }

    public void registerMethodListener(MethodListener methodListener){
        if(methodListener!=null && !methodListenerList.contains(methodListener))
            methodListenerList.add(methodListener);
    }

    public void unregisterMethodListener(MethodListener methodListener){
        if(methodListener!=null){
            methodListenerList.remove(methodListener);
        }
    }

    public void clearMethodListener(){
        methodListenerList.clear();
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

    public void setReleaseMode(boolean isReleaseMode) {
        this.isReleaseMode = isReleaseMode;
    }

    public boolean isReleaseMode() {
        return isReleaseMode;
    }

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
}
