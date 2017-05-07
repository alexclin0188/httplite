package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.Result;
import alexclin.httplite.util.Util;

/**
 * BasicCallAdapters
 *
 * @author alexclin  16/5/4 21:13
 */
class BasicCallAdapters {

    static Collection<CallAdapter> basicAdapters(ExecutorService executor){
        try {
            Class.forName("io.reactivex.Observable");
            return Arrays.asList(new ResultCallAdapter(),new AsyncCallAdapter(executor),new RxCallAdapter());
        } catch (ClassNotFoundException e) {
            return Arrays.asList(new ResultCallAdapter(),new AsyncCallAdapter(executor));
        }
    }

    private static class ResultCallAdapter implements CallAdapter {
        @Override
        public Object adapt(HttpLite lite,RequestCreator creator, final Type returnType, Object... args) throws Exception{
            return creator.createRequest(args).execute(lite,returnType);
        }

        @Override
        public boolean support(Method method) {
            return Util.isSubType(method.getGenericReturnType(),Result.class);
        }

        @Override
        public ResultType checkMethod(Method method) throws RuntimeException {
            return method.getReturnType().equals(File.class)?ResultType.File:ResultType.NotFile;
        }
    }

    @SuppressWarnings("unchecked")
    private static class AsyncCallAdapter implements CallAdapter {
        private final ExecutorService executor;

        AsyncCallAdapter(ExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public Object adapt(final HttpLite lite,final RequestCreator creator,final Type returnType,final Object[] args) throws Exception {
            if(executor!=null){
                AsyncHandle handle = new AsyncHandle() {
                    @Override
                    public void run() {
                        Request request = creator.createRequest(args);
                        setHandle(request.handle());
                        if(isCanceled()) cancel();
                        request.enqueue(lite,(Callback)args[args.length-1]);
                    }
                };
                executor.submit(handle);
                return returnType==Handle.class?handle:null;
            }else{
                Request request = creator.createRequest(args);
                request.enqueue(lite,(Callback)args[args.length-1]);
                return returnType==Handle.class?request.handle():null;
            }
        }

        @Override
        public boolean support(Method method) {
            Type[] paramTypes = method.getGenericParameterTypes();
            return paramTypes.length>0&&Util.isSubType(paramTypes[paramTypes.length-1],Callback.class);
        }

        @Override
        public ResultType checkMethod(Method method) throws RuntimeException {
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
                if(returnType != void.class&& returnType!= Handle.class){
                    throw Util.methodError(method, "the method define in the interface must return void/Handle");
                }
            }
            return Util.getTypeParameter(lastParamType)==File.class?ResultType.File:ResultType.NotFile;
        }
    }

    private static abstract class AsyncHandle implements Handle,Runnable{
        private Handle handle;
        private volatile boolean isCanceled;

        @Override
        public void cancel() {
            if(handle!=null){
                handle.cancel();
            }
            isCanceled =  true;
        }

        @Override
        public boolean isCanceled() {
            return handle==null?isCanceled:handle.isCanceled();
        }

        @Override
        public boolean isExecuted() {
            return handle!=null&&handle.isExecuted();
        }

        @Override
        public void setHandle(Handle handle) {
            this.handle = handle;
        }

        @Override
        public abstract void run();
    }
}
