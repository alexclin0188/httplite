package alexclin.httplite;


import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.ResponseFilter;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.Util;

/**
 * ResultCallback
 *
 * @author alexclin  16/1/1 19:05
 */
public abstract class ResultCallback<T> {
    protected volatile boolean isCanceled;
    protected Callback<T> callback;
    protected HttpCall call;

    protected ResultCallback(Callback<T> callback, HttpCall call) {
        this.callback = callback;
        this.call = call;
    }

    public final void onResponse(Response response){
        if(isCanceled){
            callCancelAndFailed();
            return;
        }
        ResponseFilter filter = getLite().getResponseFilter();
        if(filter!=null) filter.onResponse(getLite(),call.request,response);
        handleResponse(response);
    }

    protected abstract void handleResponse(Response response);

    protected abstract Type resultType();

    protected boolean isSuccess(Response response) {
        int code = response.code();
        return code >= 200 && code < 300;
    }

    public final void onFailed(Exception e) {
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

    protected final HttpLite getLite(){
        return call.request.lite;
    }

    abstract T parseResponse(Response response) throws Exception;

    public void callCancelAndFailed(CanceledException e){
        onCancel();
        if(e==null)
            e = new CanceledException("Request is canceled");
        postFailed(e);
    }

    public void callCancelAndFailed(){
        callCancelAndFailed(null);
    }

    protected void handleFailedCode(Response response) throws HttpException {
        String message = response.message();
        if(TextUtils.isEmpty(message)){
            try {
                message = decodeToString(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        throw new HttpException(response.code(),message);
    }

    static String decodeToString(Response response) throws IOException {
        MediaType mt = response.body().contentType();
        if(mt!=null){
            Charset cs = mt.charset(Util.UTF_8);
            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().stream(),cs==null? Util.UTF_8:cs));
            StringBuilder stringBuilder = new StringBuilder();
            String s;
            while ((s=reader.readLine())!=null){
                stringBuilder.append(s);
            }
            Util.closeQuietly(reader);
            return stringBuilder.toString();
        }
        throw new RuntimeException("Not text response body,no Content-Type in response");
    }
}
