package alexclin.httplite;

import java.util.List;
import java.util.Map;

import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.Response;
import alexclin.httplite.util.Util;

/**
 * @author xiehonglin429 on 2017/3/2.
 */

class ResponseCallback<T> implements Callback<Response> {
    private Callback<T> callback;
    private ObjectParser parser;

    ResponseCallback(Callback<T> callback, ObjectParser parser) {
        this.callback = callback;
        this.parser = parser;
    }

    @Override
    public void onSuccess(final Request req, final Map<String, List<String>> headers, final Response result) {
        try {
            if(req.handle().isCanceled()) throw new CanceledException("Request is canceled");
            final T r = parser.parseObject(result, Util.type(Callback.class,callback));
            HttpLite.runOnMain(new Runnable() {
                @Override
                public void run() {
                    callback.onSuccess(req,headers,r);
                }
            });
        } catch (Exception e) {
            onFailed(req,e);
        } finally {
            ((HandleImpl)req.handle()).setExecuted();
        }
    }

    @Override
    public void onFailed(final Request req, final Throwable e) {
        HttpLite.runOnMain(new Runnable() {
            @Override
            public void run() {
                callback.onFailed(req,e);
            }
        });
        ((HandleImpl)req.handle()).setExecuted();
    }
}
