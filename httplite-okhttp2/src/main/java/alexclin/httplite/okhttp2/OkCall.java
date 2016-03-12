package alexclin.httplite.okhttp2;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import alexclin.httplite.ResultCallback;

/**
 * OkCall
 *
 * @author alexclin 16/1/2 17:12
 */
public class OkCall extends Call {
    private ResultCallback callback;
    public OkCall(OkHttpClient client, Request originalRequest, ResultCallback callback) {
        super(client, originalRequest);
        this.callback = callback;
    }

    @Override
    public void cancel() {
        super.cancel();
        callback.onCancel();
    }
}
