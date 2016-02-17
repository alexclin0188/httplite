package alexclin.httplite.urlconnection;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;

/**
 * MockResponse
 *
 * @author alexclin
 * @date 16/1/29 20:38
 */
public class MockResponse<T> {
    private Clazz<T> clazz;
    private T result;
    private Map<String, List<String>> headers;
    private Response response;
    private Handle handle;

    private MockCall call;
    private ProgressListener mProgressListener;
    private RetryListener mRetryListener;
    private CancelListener mCancelListener;

    MockResponse(Clazz<T> clazz,MockCall call) {
        this.clazz = clazz;
        this.handle = new Handle();
        this.call = call;
        this.mProgressListener = call.request.getProgressListener();
        this.mRetryListener = call.request.getRetryListener();
        this.mCancelListener = call.request.getCancelListener();
    }

    void performCallback(Callback<T> callback) {
        callback.onSuccess(result, headers);
    }

    Response response() {
        return response;
    }

    Handle handle() {
        return handle;
    }

    T responseObject() {
        return result;
    }

    public Type responseType() {
        if (clazz != null) {
            return clazz.type();
        }
        return null;
    }

    public boolean requestObject() {
        return clazz != null;
    }

    public void mock(Response response) {
        this.response = response;
    }

    public void mock(T result, Map<String, List<String>> headers) {
        this.result = result;
        this.headers = headers;
    }

    public boolean isCanceled() {
        return call.isCanceled();
    }

    public final void mockRetry(final int tryCount, final int maxCount) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mRetryListener != null) {
                    mRetryListener.onRetry(tryCount, maxCount);
                }
            }
        });
    }

    public final void mockProgress(final long current, final long total) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mProgressListener != null) {
                    mProgressListener.onProgressUpdate(current, total);
                }
            }
        });
    }

    public final void mockCancel() {
        call.cancel();
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                if (mCancelListener != null)
                    mCancelListener.onCancel(call.request);
            }
        });
    }

    class Handle implements DownloadHandle {
        @Override
        public final void pause() {
            mockCancel();
        }

        @Override
        public final void resume() {
            if(!call.isExecuted()){
                call.reset();
                call.dispatch();
            }
        }

        @Override
        public Request request() {
            return call.request();
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public boolean isExecuted() {
            return call.isExecuted();
        }

        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }
    }
}
