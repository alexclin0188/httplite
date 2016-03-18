package alexclin.httplite.okhttp2;

import com.squareup.okhttp.Call;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.ResultCallback;

/**
 * OkHandle
 *
 * @author alexclin 16/2/17 19:46
 */
public class OkHandle implements Handle {
    private Call realCall;
    private volatile boolean isCanceled = false;
    private Request request;
    private ResultCallback callback;

    public OkHandle(final alexclin.httplite.Request request,ResultCallback callback) {
        this.request = request;
        this.callback = callback;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public void cancel() {
        if (realCall == null) {
            isCanceled = true;
        } else {
            realCall.cancel();
        }
        callback.onCancel();
    }

    @Override
    public boolean isExecuted() {
        return  realCall != null && realCall.isExecuted();
    }

    @Override
    public boolean isCanceled() {
        if (realCall == null)
            return isCanceled;
        else
            return realCall.isCanceled();
    }

    @Override
    public boolean resume() {
        return false;
    }

    void setRealCall(Call realCall) {
        this.realCall = realCall;
    }
}
