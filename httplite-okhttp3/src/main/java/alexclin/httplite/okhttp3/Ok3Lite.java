package alexclin.httplite.okhttp3;

import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.ClientSettings;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.ResponseHandler;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.util.Util;
import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;

/**
 * Ok3Lite
 *
 * @author alexclin 16/2/16 20:15
 */
public class Ok3Lite extends HttpLiteBuilder implements LiteClient{
    public static HttpLiteBuilder create() {
        return create(null);
    }

    public static HttpLiteBuilder create(OkHttpClient client) {
        return new Ok3Lite(client);
    }

    private OkHttpClient mClient;

    Ok3Lite(OkHttpClient client){
        if(client==null){
            mClient = new OkHttpClient();
        }else{
            mClient = client;
        }
    }

    @Override
    protected LiteClient initLiteClient() {
        return this;
    }

    @Override
    public Handle execute(final alexclin.httplite.Request request, final ResponseHandler callback, final Runnable preWork) {
        final OkHandle handle = new OkHandle(request,callback);
        if(preWork!=null){
            mClient.dispatcher().executorService().execute(new Runnable() {
                @Override
                public void run() {
                    preWork.run();
                    if(!handle.isCanceled()){
                        Call call = executeInternal(request,callback);
                        handle.setRealCall(call);
                    }else{
                        callback.callCancelAndFailed();
                    }
                }
            });
        }else{
            handle.setRealCall(executeInternal(request, callback));
        }
        return handle;
    }

    private Call executeInternal(final alexclin.httplite.Request request, final ResponseHandler callback){
        okhttp3.Request.Builder rb = createRequestBuilder(request);
        Call call = mClient.newCall(rb.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if("Canceled".equals(e.getMessage())){
                    callback.onFailed(new CanceledException(e));
                }else{
                    callback.onFailed(e);
                }
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                callback.onResponse(new OkResponse(response, request));
            }
        });
        return call;
    }

    private okhttp3.Request.Builder createRequestBuilder(alexclin.httplite.Request request) {
        okhttp3.Request.Builder rb = new okhttp3.Request.Builder().url(request.getUrl()).tag(request.getTag());
        Headers okheader = createHeader(request.getHeaders());
        if(okheader!=null){
            rb.headers(okheader);
        }
        switch (request.getMethod()){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(OkRequestBody.wrapperLite(request.getBody()));
                break;
            case PUT:
                rb = rb.put(OkRequestBody.wrapperLite(request.getBody()));
                break;
            case PATCH:
                rb = rb.patch(OkRequestBody.wrapperLite(request.getBody()));
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(request.getBody()==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(OkRequestBody.wrapperLite(request.getBody()));
                }
                break;
        }
        if(request.getCacheExpiredTime()>0){
            rb.cacheControl(new CacheControl.Builder().maxAge(request.getCacheExpiredTime(),TimeUnit.SECONDS).build());
        }else if(request.getCacheExpiredTime()== alexclin.httplite.Request.FORCE_CACHE){
            rb.cacheControl(CacheControl.FORCE_CACHE);
        }else if(request.getCacheExpiredTime()== alexclin.httplite.Request.NO_CACHE){
            rb.cacheControl(CacheControl.FORCE_NETWORK);
        }
        return rb;
    }

    @Override
    public alexclin.httplite.Response executeSync(alexclin.httplite.Request request) throws IOException{
        okhttp3.Request.Builder rb = createRequestBuilder(request);
        return new OkResponse(mClient.newCall(rb.build()).execute(),request);
    }

    @Override
    public void cancel(Object tag) {
        List<Call> list = mClient.dispatcher().runningCalls();
        for(Call call:list){
            if(Util.equal(tag,call.request().tag())){
                call.cancel();
            }
        }
        list = mClient.dispatcher().queuedCalls();
        for(Call call:list){
            if(Util.equal(tag,call.request().tag())){
                call.cancel();
            }
        }
    }

    @Override
    public void cancelAll() {
        mClient.dispatcher().cancelAll();
    }

    @Override
    public RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
                                           List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList){
        MultipartBody.Builder builder;
        if(boundary==null){
            builder = new MultipartBody.Builder().setType(OkMediaType.wrapperLite(type));
        }else {
            builder = new MultipartBody.Builder(boundary).setType(OkMediaType.wrapperLite(type));
        }
        if(bodyList!=null){
            for(RequestBody body:bodyList){
                builder.addPart(OkRequestBody.wrapperLite(body));
            }
        }
        if(headBodyList!=null){
            for(Pair<Map<String,List<String>>,RequestBody> bodyPair:headBodyList){
                builder.addPart(createHeader(bodyPair.first), OkRequestBody.wrapperLite(bodyPair.second));
            }
        }
        if(paramList!=null){
            for(Pair<String,String> pair:paramList){
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if(fileList!=null){
            for(Pair<String,Pair<String,RequestBody>> pair:fileList){
                builder.addFormDataPart(pair.first, pair.second.first, OkRequestBody.wrapperLite(pair.second.second));
            }
        }
        return new OkRequestBody(builder.build());
    }

    public RequestBody createFormBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList){
        FormBody.Builder builder = new FormBody.Builder();
        if(paramList!=null){
            for(Pair<String,String> param:paramList){
                builder.add(param.first,param.second);
            }
        }
        if(encodedParamList!=null){
            for(Pair<String,String> param:encodedParamList){
                builder.addEncoded(param.first,param.second);
            }
        }
        return new OkRequestBody(builder.build());
    }

    @Override
    public void setConfig(ClientSettings settings) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.followSslRedirects(settings.isFollowSslRedirects())
                .followRedirects(settings.isFollowRedirects());
        if(settings.getSocketFactory()!=null) builder.socketFactory(settings.getSocketFactory());
        if(settings.getSslSocketFactory()!=null) builder.sslSocketFactory(settings.getSslSocketFactory());
        if(settings.getHostnameVerifier()!=null) builder.hostnameVerifier(settings.getHostnameVerifier());
        if(settings.getProxySelector()!=null) builder.proxySelector(settings.getProxySelector());
        if(settings.getProxy()!=null) builder.proxy(settings.getProxy());
        builder.retryOnConnectionFailure(settings.getMaxRetryCount() > 0);
        builder.connectTimeout(settings.getConnectTimeout(), TimeUnit.MILLISECONDS);
        builder.readTimeout(settings.getReadTimeout(), TimeUnit.MILLISECONDS);
        builder.writeTimeout(settings.getWriteTimeout(), TimeUnit.MILLISECONDS);
        if(settings.getCookieHandler()!=null) builder.cookieJar(new CookieJarImpl(settings.getCookieHandler()));
        if(settings.getCacheDir()!=null){
            builder.cache(new Cache(settings.getCacheDir(), settings.getCacheMaxSize()));
        }
        mClient = builder.build();
    }

    @Override
    public void shutDown() {
        cancelAll();
        mClient.dispatcher().executorService().shutdown();
    }

    public RequestBody createRequestBody(MediaType contentType, String content) {
        okhttp3.RequestBody requestBody =
                okhttp3.RequestBody.create(OkMediaType.wrapperLite(contentType),content);
        return new OkRequestBody(requestBody);
    }

    public RequestBody createRequestBody(final MediaType contentType, final byte[] content) {
        return createRequestBody(contentType, content, 0, content.length);
    }

    public RequestBody createRequestBody(final MediaType contentType, final byte[] content,
                                         final int offset, final int byteCount) {
        okhttp3.RequestBody requestBody =
                okhttp3.RequestBody.create(OkMediaType.wrapperLite(contentType),content,offset,byteCount);
        return new OkRequestBody(requestBody);
    }

    public RequestBody createRequestBody(final MediaType contentType, final File file) {
        okhttp3.RequestBody requestBody =
                okhttp3.RequestBody.create(OkMediaType.wrapperLite(contentType),file);
        return new OkRequestBody(requestBody);
    }

    @Override
    public MediaType parse(String type) {
        okhttp3.MediaType oktype = okhttp3.MediaType.parse(type);
        return new OkMediaType(oktype);
    }

    private Headers createHeader(Map<String, List<String>> headers){
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
}
