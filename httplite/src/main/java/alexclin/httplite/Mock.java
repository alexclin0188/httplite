package alexclin.httplite;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.Response;
import alexclin.httplite.listener.ResponseBody;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.impl.ResponseImpl;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Util;

/**
 * Mock
 *
 * @author alexclin 16/1/29 20:38
 */
public class Mock<T>{
    private Request request;
    private Type type;
    private MockLite lite;
    private Callback<T> callback;

    private boolean isMocked;

    private Result<T> result;
    private Response response;

    private Runnable innerRunnable;

    Mock(Request request,Type type,MockLite lite){
        this.request = request;
        this.type = type;
        this.lite = lite;
    }

    Mock(Request request,Callback<T> callback,MockLite lite){
        this.request = request;
        this.callback = callback;
        this.lite = lite;
        this.type = Util.type(Callback.class,callback);
    }

    public boolean isCanceled() {
        return request.handle().isCanceled();
    }

    public final void mock(T result, Map<String, List<String>> headers) {
        this.result = new Result<T>(result,headers);
        this.isMocked = true;
    }

    public final void mock(Response response) {
        this.response = response;
        this.isMocked = true;
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
        mock(HttpException.SC_OK,"SUCCESS",null,stream,lite.mediaType(mediaType));
    }

    public final void mock(int code,String msg,Map<String, List<String>> headers, final InputStream stream,MediaType mediaType){
        ResponseBody body = new ResponseImpl.ResponseBodyImpl(stream,mediaType,0){
            @Override
            public long contentLength() throws IOException {
                return stream.available();
            }
        };
        mock(new ResponseImpl(request,code,msg,headers,body));
    }

    public final void mock(File inFile) throws Exception{
        mock(new FileInputStream(inFile));
    }

    public final void mock(File inFile,String mediaType) throws Exception{
        mock(new FileInputStream(inFile),mediaType);
    }

    public final void mockFailed(Exception e) throws Exception{
        throw e;
    }

    Result<T> execute() {
        execMock();
        return result;
    }

    @SuppressWarnings("unchecked")
    private void execMock() {
        try {
            lite.mock(request,this);
            if(request.handle().isCanceled())
                throw new CanceledException("Request is been canceled!");
            if(!isMocked){
                throw new IllegalStateException("Mock.mock(..) not been called");
            }
            if(result==null&&response==null){
                throw new IllegalStateException("Response mocked is null");
            }
            if(result==null){
                result = new Result<T>((T)lite.parseResponse(response,type),response.headers());
            }
        } catch (Exception e) {
            result = new Result<T>(e);
        }
    }

    Runnable getTask(){
        if(innerRunnable==null){
            innerRunnable = new InnerTask();
        }
        return innerRunnable;
    }

    private class InnerTask implements Runnable{
        @Override
        public void run() {
            execMock();
            if(result.error()!=null){
                callback.onFailed(request,result.error());
            }else{
                callback.onSuccess(request,result.headers(),result.result());
            }
        }
    }
}
