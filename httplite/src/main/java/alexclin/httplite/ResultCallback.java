package alexclin.httplite;


import java.util.List;
import java.util.Map;

import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.ResponseFilter;
import alexclin.httplite.listener.RetryListener;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 19:05
 */
public abstract class ResultCallback<T> {
    protected volatile boolean isCanceled;
    private int retryCount;
    protected Callback<T> callback;
    protected HttpCall call;

    protected ResultCallback(Callback<T> callback, HttpCall call) {
        this.callback = callback;
        this.call = call;
    }

    public final void onResponse(Response response){
        ResponseFilter filter = getLite().getResponseFilter();
        if(filter!=null) filter.onResponse(call.request,response);
        handleResponse(response);
    }

    protected abstract void handleResponse(Response response);

    public final void onFailed(Exception e) {
        if (canRetry()) {
            retry();
        } else
            postFailed(e);
    }

    public final void onRetry(final int tryCount, final int maxCount) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                RetryListener listener = call.request.retryListener;
                if (listener != null) {
                    listener.onRetry(tryCount, maxCount);
                }
            }
        });
    }

    final void onProgress(final long current, final long total) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                ProgressListener listener = call.request.progressListener;
                if (listener != null) {
                    listener.onProgressUpdate(current, total);
                }
            }
        });
    }

    public final void onCancel() {
        isCanceled = true;
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                CancelListener listener = call.request.cancelListener;
                if (listener != null)
                    listener.onCancel(call.request);
            }
        });
    }

    protected final void postFailed(final Exception e) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                callback.onFailed(call.request, e);
            }
        });
    }

    protected final void postSuccess(final T result, final Map<String,List<String>> headers) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                callback.onSuccess(result, headers);
            }
        });
    }

    protected final void retry() {
        retryCount++;
        call.excuteSelf(this);
        onRetry(retryCount, getLite().getMaxRetryCount());
    }

    final void reset(){
        retryCount = 0;
        isCanceled = false;
    }

    protected final boolean canRetry() {
        return retryCount > getLite().getMaxRetryCount() && getLite().isUseLiteRetry();
    }

    protected final HttpLite getLite(){
        return call.request.lite;
    }

    abstract T praseResponse(Response response) throws Exception;

    public void callCancelAndFailed(){
        onCancel();
        postFailed(new CanceledException("Request is canceled"));
    }
}
