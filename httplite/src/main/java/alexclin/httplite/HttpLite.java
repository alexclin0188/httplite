package alexclin.httplite;

import android.os.Handler;
import android.os.Looper;

import java.lang.reflect.Type;
import java.util.List;

import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.impl.ProgressResponse;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.listener.Response;
import alexclin.httplite.retrofit.CallAdapter;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Util;

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
    private final Retrofit retrofit;
    private final ObjectParser mObjectParser;

    private final MockLite mocker;

    HttpLite(HttpLiteBuilder builder,LiteClient client) {
        this.client = client;
        this.mObjectParser = new ObjectParser(builder.parsers);
        this.baseUrl = builder.baseUrl;
        this.isRelease = builder.isRelease;
        this.mRequestFilter = builder.mRequestFilter;
        this.retrofit = new RetrofitImpl(builder.invokers);
        this.mocker = builder.mockHandler!=null?new MockLite(builder.mockHandler,mObjectParser,builder.settings.getExecutor(),this):null;
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
        return retrofit.create(clazz,null);
    }

    public <T> T retrofit(Class<T> clazz,RequestListener filter){
        return retrofit.create(clazz,filter);
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    private RequestListener getRequestFilter() {
        return mRequestFilter;
    }

    public <T> Result<T> execute(Request request, Clazz<T> clazz) {
        return execute(request,clazz.type());
    }

    public <T> Result<T> execute(Request request, Type type) {
        request.setBaseUrl(baseUrl);
        RequestListener listener = getRequestFilter();
        if(listener!=null) listener.onRequestStart(request,type);
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
            T result = getObjectParser().parseObject(response,type);
            return new Result<T>(result,response.headers());
        } catch (Throwable e) {
            return new Result<T>(e);
        } finally {
            ((HandleImpl)request.handle()).setExecuted();
        }
    }

    public <T> void enqueue(Request request, Callback<T> callback) {
        request.setBaseUrl(baseUrl);
        RequestListener listener = getRequestFilter();
        if(listener!=null) listener.onRequestStart(request,Util.type(Callback.class,callback));
        if(mocker!=null&&mocker.needMock(request)){
            mocker.enqueue(request,callback);
        }else{
            client.enqueue(request,new ResponseCallback<T>(callback,getObjectParser()));
        }
    }

    public MediaType mediaType(String mediaType) {
        return client.mediaType(mediaType);
    }

    private class RetrofitImpl extends Retrofit{

        public RetrofitImpl(List<CallAdapter> invokers) {
            super(invokers);
        }

        @Override
        public Request.Builder setMethod(Request.Builder request, Request.Method method) {
            request.setMethod(method);
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

        @Override
        public void setBaseUrl(Request.Builder builder, String baseUrl) {
            String url = Util.appendString(baseUrl,builder.url);
            builder.url(url);
        }
    }
}
