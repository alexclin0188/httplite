package alexclin.httplite;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Type;
import java.util.List;

import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.impl.ProgressResponse;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.listener.Response;
import alexclin.httplite.listener.ResponseListener;
import alexclin.httplite.mock.MockLite;
import alexclin.httplite.retrofit.CallAdapter;
import alexclin.httplite.retrofit.MethodFilter;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.util.Clazz;

/**
 * HttpLite
 *
 * @author alexclin at 15/12/31 17:13
 */
public class HttpLite {
    private final static Handler sHandler = new Handler(Looper.getMainLooper());
    private final boolean isRelease;
    private final LiteClient client;
    private final String baseUrl;
    private final RequestListener mRequestFilter;
    private final ResponseListener mResponseFilter;
    private final Retrofit retrofit;
    private final ObjectParser mObjectParser;

    private final MockLite mocker;

    HttpLite(HttpLiteBuilder builder,LiteClient client) {
        this.client = client;
        this.mObjectParser = new ObjectParser(builder.parsers);
        this.baseUrl = builder.baseUrl;
        this.isRelease = builder.isRelease;
        this.mRequestFilter = builder.mRequestFilter;
        this.mResponseFilter = builder.mResponseFilter;
        this.retrofit = new RetrofitImpl(builder.invokers);
        this.mocker = builder.mockHandler!=null?new MockLite(builder.mockHandler):null;
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

    ObjectParser getObjectParser(){
        return mObjectParser;
    }

    public <T> T retrofit(Class<T> clazz){
        return retrofit.create(clazz,null,null);
    }

    public <T> T retrofit(Class<T> clazz,RequestListener filter){
        return retrofit.create(clazz,filter,null);
    }

    public <T> T retrofit(Class<T> clazz, RequestListener filter, MethodFilter methodFilter){
        return retrofit.create(clazz,filter,methodFilter);
    }

    public <T> T retrofit(Class<T> clazz,MethodFilter methodFilter){
        return retrofit.create(clazz,null,methodFilter);
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    RequestListener getRequestFilter() {
        return mRequestFilter;
    }

    ResponseListener getResponseFilter() {
        return mResponseFilter;
    }

    public <T> Result<T> execute(Request request, Clazz<T> clazz) {
        return execute(request,clazz.type());
    }

    public <T> Result<T> execute(Request request, Type type) {
        request.setBaseUrl(baseUrl);
        if(mocker!=null&&mocker.needMock(request)){
            return mocker.mockExecute(request,type);
        }
        try {
            Response response = client.execute(request);
            if(request.getProgressListener()!=null){
                response = new ProgressResponse(response,request.getWrapListener());
            }
            T result = getObjectParser().parseObject(response,type);
            return new Result<T>(result,response.headers());
        } catch (Throwable e) {
            return new Result<T>(e);
        }
    }

    public <T> void enqueue(Request request, Callback<T> callback) {
        request.setBaseUrl(baseUrl);
        if(mocker!=null&&mocker.needMock(request)){
            mocker.mockEnqueue(request,callback);
        }
        client.enqueue(request,new ResponseCallback<T>(callback,getObjectParser()));
    }

    private class RetrofitImpl extends Retrofit{

        public RetrofitImpl(List<CallAdapter> invokers) {
            super(invokers);
        }

        @Override
        public Request.Builder makeRequest(String baseUrl) {
            return new Request.Builder();
        }

        @Override
        public Request.Builder setMethod(Request.Builder request, Request.Method method) {
            request.setMethod(method);
            return request;
        }

        @Override
        public Request.Builder setUrl(Request.Builder request, String url) {
            request.setUrl(url);
            return request;
        }

        @Override
        public boolean isReleaseMode() {
            return HttpLite.this.isRelease;
        }

        @Override
        public HttpLite lite() {
            return HttpLite.this;
        }
    }
}
