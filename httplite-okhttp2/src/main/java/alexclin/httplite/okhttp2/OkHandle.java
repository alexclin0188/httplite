package alexclin.httplite.okhttp2;

import com.squareup.okhttp.Call;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;

/**
 * OkHandle
 *
 * @author alexclin 16/2/17 19:46
 */
public class OkHandle implements Handle {
    private Call realCall;
    private volatile boolean isCanceled = false;
    private Request request;

    public OkHandle(final alexclin.httplite.Request request) {
        this.request = request;
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
