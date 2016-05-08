package alexclin.httplite;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.listener.RequestListener;
import alexclin.httplite.listener.ResponseListener;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.mock.MockCall;
import alexclin.httplite.retrofit.CallAdapter;
import alexclin.httplite.retrofit.MethodFilter;
import alexclin.httplite.retrofit.Retrofit;
import alexclin.httplite.Call.CallFactory;
import alexclin.httplite.util.HttpMethod;

/**
 * HttpLite
 *
 * @author alexclin at 15/12/31 17:13
 */
public class HttpLite {
    private static Handler sHandler = new Handler(Looper.getMainLooper());
    private final boolean isRelease;
    private LiteClient client;
    private String baseUrl;
    private int maxRetryCount;
    private RequestListener mRequestFilter;
    private ResponseListener mResponseFilter;
    private Executor customDownloadExecutor;
    private CallFactory callFactory;
    private Retrofit retrofit;
    private ObjectParser mObjectParser;

    HttpLite(LiteClient client, String baseUrl, int maxRetryCount, CallFactory factory, boolean release,
             RequestListener requestFilter, ResponseListener responseFilter, Executor downloadExecutor, HashMap<String,ResponseParser> parserMap, List<CallAdapter> invokers) {
        this.client = client;
        this.mObjectParser = new ObjectParser(parserMap.values());
        this.baseUrl = baseUrl;
        this.maxRetryCount = maxRetryCount;
        this.isRelease = release;
        this.callFactory = factory;
        this.mRequestFilter = requestFilter;
        this.mResponseFilter = responseFilter;
        this.customDownloadExecutor = downloadExecutor;
        this.retrofit = new RetrofitImpl(invokers);
    }

    public static void postOnMain(Runnable runnable){
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

    public HttpLite setBaseUrl(String baseUrl){
        this.baseUrl = baseUrl;
        return this;
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

    ObjectParser getObjectParser(){
        return mObjectParser;
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

    Executor getCustomDownloadExecutor() {
        return customDownloadExecutor;
    }

    Call makeCall(Request request) {
        return callFactory.newCall(request);
    }

    class RetrofitImpl extends Retrofit{

        public RetrofitImpl(List<CallAdapter> invokers) {
            super(invokers);
        }

        @Override
        public Request makeRequest(String baseUrl) {
            Request request = new Request(HttpLite.this);
            request.setBaseUrl(baseUrl);
            return request;
        }

        @Override
        public Request setMethod(Request request, HttpMethod method) {
            request.method = method;
            return request;
        }

        @Override
        public Request setUrl(Request request, String url) {
            request.setUrl(url);
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
