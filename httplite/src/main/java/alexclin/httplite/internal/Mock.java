package alexclin.httplite.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import alexclin.httplite.HttpLite;
import alexclin.httplite.MediaType;
import alexclin.httplite.Response;
import alexclin.httplite.ResponseBody;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;

/**
 * Mock
 *
 * @author alexclin 16/1/29 20:38
 */
public class Mock<T> {
    private T result;
    private Map<String, List<String>> headers;

    private Response response;

    private MockTask<T> task;

    private ProgressListener mProgressListener;
    private RetryListener mRetryListener;

    Mock(MockTask<T> task) {
        this.task = task;
        this.mProgressListener = task.request().getProgressListener();
        this.mRetryListener = task.request().getRetryListener();
    }

    void performCallback(Callback<T> callback) {
        callback.onSuccess(result, headers);
    }

    T result() {
        return result;
    }

    public Type resultType() {
        if (task.clazz() != null) {
            return task.clazz().type();
        }
        return null;
    }

    public boolean isCanceled() {
        return task.isCanceled();
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

    @SuppressWarnings("unchecked")
    void processMock() throws Exception{
        if(task.clazz()==null||result!=null) return;
        if(response==null) throw new TimeoutException("No response by mock");
        if(resultType()==Response.class){
            result = (T) response;
            headers = response.headers();
        }else{
            mock(task.parseResult(response),response.headers());
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
        mock(new ByteArrayInputStream(json.getBytes()),MediaType.APPLICATION_JSON);
    }

    public final void mock(String string){
        mock(new ByteArrayInputStream(string.getBytes()),MediaType.TEXT_PLAIN);
    }

    public final void mock(InputStream stream){
        mock(stream,MediaType.APPLICATION_STREAM);
    }

    public final void mock(InputStream stream,String mediaType){
        mock(HttpException.SC_OK,"SUCCESS",null,stream,task.parse(mediaType));
    }

    public final void mock(int code,String msg,Map<String, List<String>> headers, final InputStream stream,MediaType mediaType){
        ResponseBody body = new ResponseBodyImpl(stream,mediaType,0){
            @Override
            public long contentLength() throws IOException {
                return stream.available();
            }
        };
        mock(new ResponseImpl(task.request(),code,msg,headers,body));
    }

    public final void mock(File inFile) throws Exception{
        mock(new FileInputStream(inFile));
    }

    public final void mock(File inFile,String mediaType) throws Exception{
        mock(new FileInputStream(inFile),mediaType);
    }
}
