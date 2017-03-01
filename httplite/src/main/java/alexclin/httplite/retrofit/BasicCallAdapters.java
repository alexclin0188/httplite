package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;

import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.Result;
import alexclin.httplite.util.Util;

/**
 * BasicCallAdapters
 *
 * @author alexclin  16/5/4 21:13
 */
class BasicCallAdapters {

    public static Collection<CallAdapter> basicAdapters(){
        return Arrays.asList(new AsyncCallAdapter(),new ResultCallAdapter());
    }

    private static class ResultCallAdapter implements CallAdapter {
        @Override
        public Object adapt(HttpLite lite,MethodHandler handler, final Type returnType, Object... args) throws Exception{
            return handler.createRequest(args).build().execute(lite,returnType);
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

        @Override
        public Object adapt(HttpLite lite, MethodHandler handler, Type returnType, Object... args) throws Exception {
            handler.createRequest(args).build().enqueue(lite,(Callback)args[args.length-1]);
            //TODO
//            return returnType==Handle.class?handle:null;
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
                if(returnType != void.class&& returnType!= Handle.class){
                    throw Util.methodError(method, "the method define in the interface must return void/Handle");
                }
            }
            return Util.getTypeParameter(lastParamType)==File.class?ResultType.File:ResultType.NotFile;
        }
    }

//    private static class ReturnCallAdapter implements CallAdapter {
//
//        @Override
//        public Object adapt(HttpLite lite, MethodHandler handler, Type returnType, Object... args) throws Exception {
//            return handler.createRequest(args).build().call();
//        }
//
//        @Override
//        public boolean support(Method method) {
//            return Util.getRawType(method.getReturnType())==Call.class;
//        }
//
//        @Override
//        public ResultType checkMethod(Method method) throws RuntimeException {
//            return ResultType.Any;
//        }
//    }
}
