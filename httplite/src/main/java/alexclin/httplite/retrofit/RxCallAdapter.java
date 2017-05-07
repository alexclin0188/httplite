package alexclin.httplite.retrofit;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Callable;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.Result;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Util;
import io.reactivex.Observable;

/**
 * RxCallAdapter
 *
 * @author alexclin  16/3/18 23:03
 */
class RxCallAdapter implements CallAdapter {

    RxCallAdapter() {}

    @Override
    public Object adapt(HttpLite lite, RequestCreator creator, Type returnType, Object... args) throws Exception {
        return Observable.fromCallable(CallOnSubscribe.newInstance(lite,creator,returnType,args));
    }

    @Override
    public boolean support(Method method) {
        return method.getReturnType() == Observable.class;
    }

    @Override
    public ResultType checkMethod(Method method) throws RuntimeException {
        Type type = Util.getTypeParameter(method.getGenericReturnType());
        if (Util.isSubType(type, Result.class)) {
            return Util.getTypeParameter(type) == File.class ? ResultType.File : ResultType.NotFile;
        } else {
            return type == File.class ? ResultType.File : ResultType.NotFile;
        }
    }

    private static class CallOnSubscribe<R> implements Callable<R> {
        private Request request;
        private Type observableType;
        private HttpLite lite;

        CallOnSubscribe(Request request, Type observableType, HttpLite lite) {
            this.request = request;
            this.observableType = observableType;
            this.lite = lite;
        }

        static <T> CallOnSubscribe<T> newInstance(HttpLite lite, RequestCreator creator, Type returnType, Object... args){
            Request request = creator.createRequest(args);
            Type observableType = Util.getTypeParameter(returnType);
            return new CallOnSubscribe<>(request, observableType,lite);
        }

        @Override @SuppressWarnings("unchecked")
        public R call() throws Exception {
            Class<?> observableClass = Util.getRawType(observableType);
            if (observableClass == Result.class) {
                final Type rT = Util.getTypeParameter(observableType);
                return (R)request.execute(lite, new Clazz<Object>() {
                    @Override
                    public Type type() {
                        return rT;
                    }
                });
            } else {
                Result<R> result = request.execute(lite, new Clazz<R>() {
                    @Override
                    public Type type() {
                        return observableType;
                    }
                });
                if (result.isSuccessful())
                    return result.result();
                else
                    throw result.error();
            }
        }
    }
}
