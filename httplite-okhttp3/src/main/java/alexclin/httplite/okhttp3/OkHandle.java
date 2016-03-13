package alexclin.httplite.okhttp3;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.ResultCallback;
import okhttp3.Call;

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

    public OkHandle(final Request request,ResultCallback callback) {
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
        if (realCall == null)
            return false;
        else
            return realCall.isExecuted();
    }

    @Override
    public boolean isCanceled() {
        if (realCall == null)
            return isCanceled;
        else
            return realCall.isCanceled();
    }

    void setRealCall(Call realCall) {
        this.realCall = realCall;
    }
}
