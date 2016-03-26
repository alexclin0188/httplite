package alexclin.httplite.rx;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import alexclin.httplite.Call;
import alexclin.httplite.Clazz;
import alexclin.httplite.Result;
import alexclin.httplite.retrofit.Invoker;
import alexclin.httplite.util.Util;
import rx.Observable;
import rx.Subscriber;

/**
 * RxInvoker
 *
 * @author alexclin  16/3/18 23:03
 */
public class RxInvoker implements Invoker {

    @Override
    public Object invoke(Call call, Type returnType, Object... args) throws Exception {
        return invokeInner(call,returnType);
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> invokeInner(Call call,Type returnType){
        final Type observableType = Util.getTypeParameter(returnType);
        Class<?> observableClass = Util.getRawType(observableType);
        if(observableClass==Result.class){
            final Type rT = Util.getTypeParameter(observableType);
            return (Observable<T>) createResultObservable(call,rT);
        }else{
            return createObservable(call, observableType);
        }
    }

    private <T> Observable<T> createObservable(final Call call, final Type observableType) {
        return Observable.create(new CallOnSubscribe<T>(new ExecuteAble<T>() {
            @Override
            public T execute() throws Exception {
                return call.sync(new Clazz<T>() {
                    @Override
                    public Type type() {
                        return observableType;
                    }
                });
            }
        }));
    }

    private <R> Observable<Result<R>> createResultObservable(final Call call,final Type rT) {
        return Observable.create(new CallOnSubscribe<Result<R>>(new ExecuteAble<Result<R>>() {
            @Override
            public Result<R> execute() throws Exception {
                return call.syncResult(new Clazz<R>() {
                    @Override
                    public Type type() {
                        return rT;
                    }
                });
            }
        }));
    }

    @Override
    public boolean support(Method method) {
        return method.getReturnType()==Observable.class;
    }

    @Override
    public boolean checkMethod(Method method) throws RuntimeException {
        Type type = Util.getTypeParameter(method.getGenericReturnType());
        if(Util.isSubType(type,Result.class)){
            return Util.getTypeParameter(type)==File.class;
        }else{
            return type== File.class;
        }
    }

    private static class CallOnSubscribe<R> implements Observable.OnSubscribe<R>{
        private ExecuteAble<R> wrapper;

        public CallOnSubscribe(ExecuteAble<R> wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public void call(Subscriber<? super R> subscriber) {
            try {
                R r = wrapper.execute();
                if (!subscriber.isUnsubscribed())
                    subscriber.onNext(r);
            } catch (Exception e) {
                if (!subscriber.isUnsubscribed())
                    subscriber.onError(e);
            }
            if (!subscriber.isUnsubscribed())
                subscriber.onCompleted();
            if (!subscriber.isUnsubscribed()) {
                subscriber.unsubscribe();
            }
        }
    }

    private interface ExecuteAble<E>{
        E execute() throws Exception;
    }
}
