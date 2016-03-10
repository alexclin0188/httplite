package alexclin.httplite.okhttp3;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import okhttp3.Call;

/**
 * HandleImpl
 *
 * @author alexclin
 * @date 16/2/17 19:46
 */
public class HandleImpl implements Handle {
    private Call realCall;
    private volatile boolean isCanceled = false;
    private Request request;

    public HandleImpl(final Request request) {
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
