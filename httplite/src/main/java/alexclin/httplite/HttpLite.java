package alexclin.httplite;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Type;

import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.impl.ProgressResponse;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.RequestInterceptor;
import alexclin.httplite.listener.Response;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Util;

/**
 * HttpLite
 *
 * @author alexclin at 15/12/31 17:13
 */
public class HttpLite {
    private final static Handler sHandler = new Handler(Looper.getMainLooper());
    private final LiteClient client;
    private final String baseUrl;
    private final RequestInterceptor mRequestInterceptor;
    private final ObjectParser mObjectParser;

    private final MockLite mocker;

    HttpLite(HttpLiteBuilder builder,LiteClient client) {
        this.client = client;
        this.mObjectParser = new ObjectParser(builder.parsers);
        this.baseUrl = builder.baseUrl;
        this.mRequestInterceptor = builder.mRequestInterceptor;
        this.mocker = builder.mockHandler!=null?new MockLite(builder.mockHandler,mObjectParser,builder.mockExecutor,this):null;
    }

    public static void runOnMain(Runnable runnable){
        if(Thread.currentThread()==Looper.getMainLooper().getThread()){
            runnable.run();
        }else{
            sHandler.post(runnable);
        }
    }

    public static Handler mainHandler(){
        return sHandler;
    }

    public String getBaseUrl(){
        return baseUrl;
    }

    public void cancel(Object tag){
        this.mocker.cancel(tag);
        this.client.cancel(tag);
    }

    public void cancelAll(){
        this.mocker.cancelAll();
        this.client.cancelAll();
    }

    public void shutDown(){
        this.mocker.shutDown();
        this.client.shutDown();
    }

    private RequestInterceptor getRequestFilter() {
        return mRequestInterceptor;
    }

    <T> Result<T> execute(Request request, Clazz<T> clazz) {
        return execute(request,clazz.type());
    }

    <T> Result<T> execute(Request request, Type type) {
        RequestInterceptor listener = getRequestFilter();
        if(listener!=null)
            request = listener.interceptRequest(request,type);
        request.ensureFullUrl(baseUrl);
        if(mocker!=null&&mocker.needMock(request)){
            Result<T> result = mocker.execute(request,type);
            ((HandleImpl)request.handle()).setExecuted();
            return result;
        }
        try {
            Response response = client.execute(request);
            if(request.getProgressListener()!=null){
                response = new ProgressResponse(response,request.getWrapListener());
            }
            T result = mObjectParser.parseObject(response,type);
            return new Result<T>(result,response.headers());
        } catch (Exception e) {
            return new Result<T>(e);
        } finally {
            ((HandleImpl)request.handle()).setExecuted();
        }
    }

    public <T> void enqueue(Request request, Callback<T> callback) {
        RequestInterceptor listener = getRequestFilter();
        if(listener!=null)
            request = listener.interceptRequest(request,Util.type(Callback.class,callback));
        request.ensureFullUrl(baseUrl);
        if(mocker!=null&&mocker.needMock(request)){
            mocker.enqueue(request,callback);
        }else{
            client.enqueue(request,new ResponseCallback<T>(callback,mObjectParser));
        }
    }

    public MediaType mediaType(String mediaType) {
        return client.mediaType(mediaType);
    }
}
