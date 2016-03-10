package alexclin.httplite.mock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;

/**
 * Mock
 *
 * @author alexclin 16/1/29 20:38
 */
public class Mock<T> {
    private Clazz<T> clazz;
    private T result;
    private Map<String, List<String>> headers;
    private Handle handle;

    private Response response;
    private MockCall call;

    private ProgressListener mProgressListener;
    private RetryListener mRetryListener;
    private CancelListener mCancelListener;

    Mock(Clazz<T> clazz, MockCall call) {
        this.clazz = clazz;
        this.handle = new Handle();
        this.call = call;
        this.mProgressListener = call.request().getProgressListener();
        this.mRetryListener = call.request().getRetryListener();
        this.mCancelListener = call.request().getCancelListener();
    }

    void performCallback(Callback<T> callback) {
        callback.onSuccess(result, headers);
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
                    mCancelListener.onCancel(call.request());
            }
        });
    }

    void processMock() throws Exception{
        if(clazz==null||result!=null) return;
        if(response==null) throw new TimeoutException("No response by mock");
        if(responseType()==Response.class){
            result = (T) response;
            headers = response.headers();
        }else{
            mock(call.parseResultFrom(response,clazz),response.headers());
        }
    }

    public final void mock(T result, Map<String, List<String>> headers) {
        this.result = result;
        this.headers = headers;
    }

    public final void mock(Response response) {
        this.response = response;
    }

    public final void mockJson(String json){
        mock(HttpException.SC_OK,"SUCCESS",null,new ByteArrayInputStream(json.getBytes()),call.prase(MediaType.APPLICATION_JSON));
    }

    public final void mock(String string){
        mock(HttpException.SC_OK,"SUCCESS",null,new ByteArrayInputStream(string.getBytes()),call.prase(MediaType.TEXT_PLAIN));
    }

    public final void mock(InputStream stream){
        mock(stream,MediaType.APPLICATION_STREAM);
    }

    public final void mock(InputStream stream,String mediaType){
        mock(HttpException.SC_OK,"SUCCESS",null,stream,call.prase(mediaType));
    }

    public final void mock(int code,String msg,Map<String, List<String>> headers,InputStream stream,MediaType mediaType){
        mock(new MockResponse(call.request(),0,"",headers,stream,mediaType));
    }

    public final void mock(File inFile) throws Exception{
        mock(new FileInputStream(inFile));
    }

    public final void mock(File inFile,String mediaType) throws Exception{
        mock(new FileInputStream(inFile),mediaType);
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
