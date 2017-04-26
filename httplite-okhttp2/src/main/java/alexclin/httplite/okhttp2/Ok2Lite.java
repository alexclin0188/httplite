package alexclin.httplite.okhttp2;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.net.CookieHandler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.Handle;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.exception.IllegalOperationException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.util.ClientSettings;

/**
 * Ok2Lite
 *
 * @author alexclin 16/1/1 17:16
 */
public class Ok2Lite extends HttpLiteBuilder implements LiteClient{
    private static final Object ALL_TAG = new Object(){
        @Override
        public boolean equals(Object o) {
            return true;
        }
    };
    private OkHttpClient mClient;
    private Ok2Factory mFactory;

    Ok2Lite(OkHttpClient client){
        if(client==null){
            mClient = new OkHttpClient();
        }else{
            mClient = client;
        }
        mFactory = new Ok2Factory();
    }

    public static HttpLiteBuilder create() {
        return create(null);
    }

    public static HttpLiteBuilder create(OkHttpClient client) {
        return new Ok2Lite(client);
    }


    @Override
    public Response execute(Request request) throws Exception {
        Call call = mClient.newCall(makeRequest(request));
        request.handle().setHandle(new CallHandle(call));
        return new OkResponse(call.execute(),request);
    }

    @Override
    public void enqueue(final Request request,final Callback<Response> callback) {
        Call call = mClient.newCall(makeRequest(request));
        request.handle().setHandle(new CallHandle(call));
        com.squareup.okhttp.Callback ok2Callback = new com.squareup.okhttp.Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request okReq, IOException e) {
                Exception throwable;
                if("Canceled".equals(e.getMessage())){
                    throwable = new CanceledException(e);
                }else{
                    throwable = e;
                }
                callback.onFailed(request,throwable);
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                callback.onSuccess(request,response.headers().toMultimap(),new OkResponse(response,request));
            }
        };
        call.enqueue(ok2Callback);
    }

    @Override
    public void cancel(Object tag) {
        mClient.cancel(tag);
    }

    @Override
    public void cancelAll() {
        mClient.getDispatcher().cancel(ALL_TAG);
    }

    @Override
    protected LiteClient initClient(ClientSettings settings) {
        mClient.setProxy(settings.getProxy()).setProxySelector(settings.getProxySelector()).setSocketFactory(settings.getSocketFactory())
                .setSslSocketFactory(settings.getSslSocketFactory())
                .setHostnameVerifier(settings.getHostnameVerifier()).setFollowSslRedirects(settings.isFollowSslRedirects())
                .setFollowRedirects(settings.isFollowRedirects());
        mClient.setRetryOnConnectionFailure(settings.getMaxRetryCount()>0);
        mClient.setConnectTimeout(settings.getConnectTimeout(), TimeUnit.MILLISECONDS);
        mClient.setReadTimeout(settings.getReadTimeout(),TimeUnit.MILLISECONDS);
        mClient.setWriteTimeout(settings.getWriteTimeout(), TimeUnit.MILLISECONDS);
        Object cookieHandler = settings.getCookieHandler();
        if(cookieHandler instanceof CookieHandler){
            mClient.setCookieHandler((CookieHandler) cookieHandler);
        }else if(cookieHandler!=null){
            throw new IllegalArgumentException("Only support type CookieHandler implement for cookie settings");
        }
        if(settings.getCacheDir()!=null){
            mClient.setCache(new Cache(settings.getCacheDir(),settings.getCacheMaxSize()));
        }
        return this;
    }

    @Override
    public void shutDown() {
        cancelAll();
        mClient.getDispatcher().getExecutorService().shutdown();
    }

    @Override
    public MediaType mediaType(String type) {
        return new OkMediaType(com.squareup.okhttp.MediaType.parse(type));
    }

    private com.squareup.okhttp.Request makeRequest(Request request){
        com.squareup.okhttp.Request.Builder rb = new com.squareup.okhttp.Request.Builder().url(request.getUrl()).tag(request.getTag());
        Headers headers = createHeader(request.getHeaders());
        if(headers!=null) rb.headers(headers);
        alexclin.httplite.RequestBody liteBody = request.getRequestBody();
        RequestBody requestBody = null;
        if(liteBody!=null){
            requestBody = mFactory.convertBody(liteBody,request.getWrapListener());
        }
        switch (request.getMethod()){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(requestBody);
                break;
            case PUT:
                rb = rb.put(requestBody);
                break;
            case PATCH:
                rb = rb.patch(requestBody);
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(requestBody==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(requestBody);
                }
                break;
        }
        if(request.getCacheExpiredTime()>0){
            rb.cacheControl(new CacheControl.Builder().maxAge(request.getCacheExpiredTime(), TimeUnit.SECONDS).build());
        }else if(request.getCacheExpiredTime()== alexclin.httplite.Request.FORCE_CACHE){
            rb.cacheControl(CacheControl.FORCE_CACHE);
        }else if(request.getCacheExpiredTime()== alexclin.httplite.Request.NO_CACHE){
            rb.cacheControl(CacheControl.FORCE_NETWORK);
        }
        return rb.build();
    }

    static Headers createHeader(Map<String, List<String>> headers){
        if(headers!=null&&!headers.isEmpty()){
            Headers.Builder hb = new Headers.Builder();
            for(String key:headers.keySet()){
                List<String> values = headers.get(key);
                for(String value:values){
                    hb.add(key,value);
                }
            }
            return hb.build();
        }
        return null;
    }

    private static class CallHandle implements Handle {
        private Call call;

        private CallHandle(Call call) {
            this.call = call;
        }

        @Override
        public void cancel() {
            call.cancel();
        }

        @Override
        public boolean isCanceled() {
            return call.isCanceled();
        }

        @Override
        public boolean isExecuted() {
            return call.isExecuted();
        }

        @Override
        public void setHandle(Handle handle) {
            throw new IllegalOperationException("not support method");
        }
    }
}
