package alexclin.httplite.retrofit;

import alexclin.httplite.Call;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.util.Result;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.util.Util;

/**
 * RetrofitCall
 *
 * @author alexclin  16/3/24 22:49
 */
public class RetrofitCall extends Call {
    private Call realCall;
    private RequestFilter filter;
    private Retrofit retrofit;

    public RetrofitCall(Call realCall, RequestFilter filter, Retrofit retrofit) {
        super(realCall.request());
        this.realCall = realCall;
        this.filter = filter;
        this.retrofit = retrofit;
    }

    @Override
    public <T> Handle async(boolean callOnMain, Callback<T> callback) {
        if(filter!=null) filter.onRequest(retrofit.lite(),request(), Util.type(Callback.class, callback));
        return realCall.async(callOnMain,callback);
    }

    @Override
    public <T> T sync(final Clazz<T> clazz) throws Exception {
        if(filter!=null) HttpLite.postOnMain(new Runnable() {
            @Override
            public void run() {
                filter.onRequest(retrofit.lite(), realCall.request(), clazz.type());
            }
        });
        return realCall.sync(clazz);
    }

    @Override
    public <T> Result<T> syncResult(Clazz<T> clazz) {
        return realCall.syncResult(clazz);
    }

    @Override
    public Request request() {
        return realCall.request();
    }
}
