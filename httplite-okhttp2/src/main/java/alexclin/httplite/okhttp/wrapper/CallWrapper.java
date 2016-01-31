package alexclin.httplite.okhttp.wrapper;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import alexclin.httplite.ResultCallback;

/**
 * alexclin.httplite.okhttp.wrapper
 *
 * @author alexclin
 * @date 16/1/2 17:12
 */
public class CallWrapper extends Call {
    private ResultCallback callback;
    public CallWrapper(OkHttpClient client, Request originalRequest,ResultCallback callback) {
        super(client, originalRequest);
        this.callback = callback;
    }

    @Override
    public void cancel() {
        super.cancel();
        callback.onCancel();
    }
}
