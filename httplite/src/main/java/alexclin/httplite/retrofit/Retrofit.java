package alexclin.httplite.retrofit;

import android.os.Build;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Call;
import alexclin.httplite.Clazz;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.Result;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.util.Util;

/**
 * Retrofit
 *
 * @author alexclin 16/1/5 23:06
 */
public abstract class Retrofit {

    private final Map<Method,MethodHandler> methodHandlerCache = new LinkedHashMap<>();  //TODO LinkedHashMap合不合适？

    private MethodListener methodListener;
    private List<Invoker> mInvokers;

    public Retrofit(List<Invoker> invokers) {
        mInvokers = new ArrayList<>();
        if(invokers!=null){
            mInvokers.addAll(invokers);
        }
        mInvokers.add(new AsyncInvoker());
        mInvokers.add(new SyncInvoker());
    }

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

    MethodHandler loadMethodHandler(Method method) {
        MethodHandler handler;
        synchronized (methodHandlerCache) {
            handler = methodHandlerCache.get(method);
            if (handler == null) {
                handler = new MethodHandler(method,!isReleaseMode(),searchInvoker(method));
                methodHandlerCache.put(method, handler);
            }
        }
        return handler;
    }

    public abstract Request makeRequest(String baseUrl);

    public abstract Request setMethod(Request request,alexclin.httplite.Method method);

    public abstract Request setUrl(Request request, String url);

    public abstract Call makeCall(Request request);

    public abstract boolean isReleaseMode();

    public abstract HttpLite lite();

    public static void registerParamterProcessor(ParameterProcessor processor) {
        if (processor != null && !ProcessorFactory.parameterProcessorList.contains(processor)) {
            ProcessorFactory.parameterProcessorList.add(processor);
        }
    }

    public static void unregisterParamterProcessor(ParameterProcessor processor) {
        if (processor != null) {
            ProcessorFactory.parameterProcessorList.remove(processor);
        }
    }

    public static void unregisterParamterProcessor(Class<? extends ParameterProcessor> clazz) {
        Iterator<ParameterProcessor> iterator = ProcessorFactory.parameterProcessorList.iterator();
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

    private Invoker searchInvoker(Method method) throws RuntimeException{
        for(Invoker invoker:mInvokers){
            if(invoker.support(method)){
                if(!isReleaseMode()) invoker.checkMethod(method);
                return invoker;
            }
        }
        return null;
    }

    private class SyncInvoker implements Invoker {
        @Override
        public Object invoke(Call call,final Type returnType, Object... args) throws Exception{
            Clazz clazz = new Clazz() {
                @Override
                public Type type() {
                    return returnType;
                }
            };
            if(Util.isSubType(returnType, Result.class)){
                return call.syncResult(clazz);
            }else{
                return call.sync(clazz);
            }
        }

        @Override
        public boolean support(Method method) {
            return true;
        }

        @Override
        public void checkMethod(Method method) throws RuntimeException {
            if(method.getReturnType()!=Result.class){
                Class[] exceptionClasses = method.getExceptionTypes();
                if(exceptionClasses.length!=1|| exceptionClasses[0]!=Exception.class){
                    throw Util.methodError(method,"Sync method must declare throws Exception");
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private class AsyncInvoker implements Invoker {
        @Override
        public Object invoke(Call call, Type returnType, Object... args) throws Exception{
            Handle obj = call.async(true,(Callback)args[args.length-1]);
            return returnType==void.class?null:obj;
        }

        @Override
        public boolean support(Method method) {
            Class[] paramTyps = method.getParameterTypes();
            boolean isSupport = (paramTyps[paramTyps.length-1]==Callback.class)&&
                    (method.getReturnType() == Callback.class);
            return isSupport;
        }

        @Override
        public void checkMethod(Method method) throws RuntimeException {
            Type returnType = method.getGenericReturnType();
            Type[] methodParameterTypes  = method.getGenericParameterTypes();
            if(methodParameterTypes.length==0){
                throw new IllegalArgumentException("the method define in the interface must have at least one parameter as Callback<T> or Clazz<T>");
            }
            Type lastParamType = methodParameterTypes[methodParameterTypes.length-1];
            if(Util.hasUnresolvableType(lastParamType)){
                throw Util.methodError(method,
                        "Method lastParamType must not include a type variable or wildcard: %s", lastParamType);
            }
            if(Util.isSubType(lastParamType, Callback.class)){
                if(returnType != void.class && returnType != Handle.class){
                    throw Util.methodError(method, "the method define in the interface must return void or Handle");
                }
            }
        }
    }
}
