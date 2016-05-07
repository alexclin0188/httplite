package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

import alexclin.httplite.Call;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Result;
import alexclin.httplite.util.Util;

/**
 * BasicCallAdapters
 *
 * @author alexclin  16/5/4 21:13
 */
class BasicCallAdapters {

    public static Collection<CallAdapter> basicAdapters(){
        return Arrays.asList(new ReturnCallAdapter(),new AsyncCallAdapter(),new SyncCallAdapter());
    }

    private static class SyncCallAdapter implements CallAdapter {
        @Override
        public Object adapt(Call call, final Type returnType, Object... args) throws Exception{
            Clazz clazz = new Clazz<Object>() {
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
        public ResultType checkMethod(Method method) throws RuntimeException {
            if(method.getReturnType()!=Result.class){
                Class[] exceptionClasses = method.getExceptionTypes();
                if(exceptionClasses.length!=1|| exceptionClasses[0]!=Exception.class){
                    throw Util.methodError(method,"Sync method must declare throws Exception");
                }
                return Util.getTypeParameter(method.getGenericReturnType())==File.class?ResultType.File:ResultType.NotFile;
            }else{
                return method.getReturnType().equals(File.class)?ResultType.File:ResultType.NotFile;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static class AsyncCallAdapter implements CallAdapter {
        @Override
        public Object adapt(Call call, Type returnType, Object... args) throws Exception{
            call.async(true,(Callback)args[args.length-1]);
            return null;
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
                if(returnType != void.class){
                    throw Util.methodError(method, "the method define in the interface must return void");
                }
            }
            return Util.getTypeParameter(lastParamType)==File.class?ResultType.File:ResultType.NotFile;
        }
    }

    private static class ReturnCallAdapter implements CallAdapter {

        @Override
        public Object adapt(Call call, Type returnType, Object... args) throws Exception {
            return call;
        }

        @Override
        public boolean support(Method method) {
            return Util.getRawType(method.getReturnType())==Call.class;
        }

        @Override
        public ResultType checkMethod(Method method) throws RuntimeException {
            return ResultType.Any;
        }
    }
}
