package alexclin.httplite;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.listener.ResponseFilter;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.internal.MockCall;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.Call.CallFactory;

/**
 * HttpLite
 *
 * @author alexclin at 15/12/31 17:13
 */
public class HttpLite {
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private LiteClient client;
    private HashMap<String,ResponseParser> parserMap;
    private String baseUrl;
    private int maxRetryCount;
    private RequestFilter mRequestFilter;
    private ResponseFilter mResponseFilter;
    private Executor customDownloadExecutor;
    private CallFactory callFactory;

    private Retrofit retrofit;
    private final boolean isRelease;

    HttpLite(LiteClient client, String baseUrl,int maxRetryCount,CallFactory factory,boolean release,
             RequestFilter requestFilter,ResponseFilter responseFilter,Executor downloadExecutor,HashMap<String,ResponseParser> parserMap) {
        this.client = client;
        this.parserMap = parserMap;
        this.baseUrl = baseUrl;
        this.maxRetryCount = maxRetryCount;
        this.isRelease = release;
        this.callFactory = factory;
        this.mRequestFilter = requestFilter;
        this.mResponseFilter = responseFilter;
        this.customDownloadExecutor = downloadExecutor;
    }

    public HttpLite setBaseUrl(String baseUrl){
        this.baseUrl = baseUrl;
        return this;
    }

    public String getBaseUrl(){
        return baseUrl;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public Request url(String url){
        if(url==null) return null;
        return new Request(this,url);
    }

    public LiteClient getClient(){
        return client;
    }

    public void cancel(Object tag){
        if(callFactory instanceof MockCall.MockFactory){
            ((MockCall.MockFactory)callFactory).cancel(tag);
        }else{
            this.client.cancel(tag);
        }
    }

    public void cancelAll(){
        if(callFactory instanceof MockCall.MockFactory){
            ((MockCall.MockFactory)callFactory).cancelAll();
        }else{
            this.client.cancelAll();
        }
    }

    public void shutDown(){
        if(callFactory instanceof MockCall.MockFactory){
            ((MockCall.MockFactory)callFactory).shutDown();
        }else{
            this.client.shutDown();
        }
    }

    public static void postOnMain(Runnable runnable){
        if(Thread.currentThread()==Looper.getMainLooper().getThread()){
            runnable.run();
        }else{
            sHandler.post(runnable);
        }
    }

    Collection<ResponseParser> getParsers(){
        return parserMap.values();
    }

    public RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
                                    List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList){
        return client.createMultipartBody(boundary, type, bodyList, headBodyList, paramList, fileList);
    }

    public RequestBody createFormBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList){
        return client.createFormBody(paramList, encodedParamList);
    }


    public RequestBody createRequestBody(MediaType contentType, String content){
        return client.createRequestBody(contentType, content);
    }

    public RequestBody createRequestBody(final MediaType contentType, final byte[] content){
        return client.createRequestBody(contentType, content);
    }

    public RequestBody createRequestBody(final MediaType contentType, final byte[] content,
                                  final int offset, final int byteCount){
        return client.createRequestBody(contentType, content, offset, byteCount);
    }

    public RequestBody createRequestBody(final MediaType contentType, final File file){
        return client.createRequestBody(contentType, file);
    }

    public MediaType parse(String type){
        return client.parse(type);
    }

    public <T> T retrofit(Class<T> clazz){
        return retrofit(clazz, null);
    }

    public <T> T retrofit(Class<T> clazz,RequestFilter filter){
        if(retrofit==null)
            synchronized (this){
                if(retrofit==null) retrofit = new RetrofitImpl();
            }
        return retrofit.create(clazz,filter);
    }

    public Retrofit getRetrofit() {
        return retrofit;
    }

    RequestFilter getRequestFilter() {
        return mRequestFilter;
    }

    ResponseFilter getResponseFilter() {
        return mResponseFilter;
    }

    Executor getCustomDownloadExecutor() {
        return customDownloadExecutor;
    }

    Call makeCall(Request request) {
        return callFactory.newCall(request);
    }

    class RetrofitImpl extends Retrofit{

        @Override
        public Request makeRequest(String baseUrl) {
            Request request = new Request(HttpLite.this);
            request.baseUrl = baseUrl;
            return request;
        }

        @Override
        public Request setMethod(Request request, Method method) {
            request.method = method;
            return request;
        }

        @Override
        public Request setUrl(Request request, String url) {
            request.url = url;
            return request;
        }

        @Override
        public Call makeCall(Request request) {
            return request.method(request.method,null);
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
