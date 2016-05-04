package alexclin.httplite.rx;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicBoolean;

import alexclin.httplite.Call;
import alexclin.httplite.Request;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Result;
import alexclin.httplite.retrofit.CallAdapter;
import alexclin.httplite.util.Util;
import rx.Observable;
import rx.Producer;
import rx.Subscriber;
import rx.Subscription;
import rx.exceptions.Exceptions;
import rx.functions.Func3;

/**
 * RxCallAdapter
 *
 * @author alexclin  16/3/18 23:03
 */
public class RxCallAdapter implements CallAdapter {

    private Func3<Request,Type,Observable<?>,Observable<?>> invokeFilter;

    public RxCallAdapter(Func3<Request, Type, Observable<?>, Observable<?>> invokeFilter) {
        this.invokeFilter = invokeFilter;
    }

    public RxCallAdapter() {
    }

    @Override
    public Object adapt(Call call, Type returnType, Object... args) throws Exception {
        Observable<?> observable = invokeInner(call,returnType);
        if(invokeFilter!=null){
            observable = invokeFilter.call(call.request(),Util.getTypeParameter(returnType),observable);
        }
        return observable;
    }

    @SuppressWarnings("unchecked")
    private <T> Observable<T> invokeInner(Call call,Type returnType){
        final Type observableType = Util.getTypeParameter(returnType);
        return Observable.create(new CallOnSubscribe<T>(call,observableType));
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
        private Call call;
        private Type observableType;

        public CallOnSubscribe(Call call,Type observableType) {
            this.call = call;
            this.observableType = observableType;
        }

        @Override
        public void call(Subscriber<? super R> subscriber) {
            RequestArbiter<R> requestArbiter = new RequestArbiter<>(call,subscriber,observableType);
            subscriber.add(requestArbiter);
            subscriber.setProducer(requestArbiter);
        }
    }

    static final class RequestArbiter<T> extends AtomicBoolean implements Subscription, Producer{
        private Call call;
        private Subscriber<? super T> subscriber;
        private Type observableType;

        public RequestArbiter(Call call, Subscriber<? super T> subscriber, Type observableType) {
            this.call = call;
            this.subscriber = subscriber;
            this.observableType = observableType;
        }

        @Override @SuppressWarnings("unchecked")
        public void request(long n) {
            if (n < 0) throw new IllegalArgumentException("n < 0: " + n);
            if (n == 0) return; // Nothing to do when requesting 0.
            if (!compareAndSet(false, true)) return; // Request was already triggered.

            try {
                T response;
                Class<?> observableClass = Util.getRawType(observableType);
                if(observableClass==Result.class){
                    final Type rT = Util.getTypeParameter(observableType);
                    response = (T)call.syncResult(new Clazz<R>() {
                        @Override
                        public Type type() {
                            return rT;
                        }
                    });
                }else{
                    response = call.sync(new Clazz<T>() {
                        @Override
                        public Type type() {
                            return observableType;
                        }
                    });
                }
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onNext(response);
                }
            } catch (Throwable t) {
                Exceptions.throwIfFatal(t);
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(t);
                }
                return;
            }

            if (!subscriber.isUnsubscribed()) {
                subscriber.onCompleted();
            }
        }

        @Override public void unsubscribe() {
            call.cancel();
        }

        @Override public boolean isUnsubscribed() {
            return call.isCanceled();
        }
    }
}
