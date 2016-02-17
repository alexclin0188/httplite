package alexclin.httplite.okhttp2;

import android.util.Pair;

import com.squareup.okhttp.Cache;
import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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
import alexclin.httplite.ResultCallback;

/**
 * alexclin.httplite.okhttp
 *
 * @author alexclin
 * @date 16/1/1 17:16
 */
public class Ok2Lite extends HttpLiteBuilder implements LiteClient{
    public static HttpLiteBuilder create() {
        return create(null);
    }

    public static HttpLiteBuilder create(OkHttpClient client) {
        return new Ok2Lite(client);
    }

    private OkHttpClient mClient;

    Ok2Lite(OkHttpClient client){
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
    public Handle execute(final alexclin.httplite.Request request, final ResultCallback callback, final Runnable preWork) {
        final HandleImpl handle = new HandleImpl(request);
        if(preWork!=null){
            mClient.getDispatcher().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    preWork.run();
                    if(!handle.isCanceled()){
                        Call call = executeInternal(request,callback);
                        handle.setRealCall(call);
                    }
                }
            });
        }else{
            handle.setRealCall(executeInternal(request, callback));
        }
        return handle;
    }

    private Call executeInternal(final alexclin.httplite.Request request, final ResultCallback callback){
        com.squareup.okhttp.Request.Builder rb = Ok2Lite.createRequestBuilder(request);
        Call realCall = new CallWrapper(mClient,rb.build(),callback);
        realCall.enqueue(new Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request request, IOException e) {
                callback.onFailed(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                callback.onResponse(new ResponseWrapper(response, request));
            }
        });
        return realCall;
    }

    static Request.Builder createRequestBuilder(alexclin.httplite.Request request) {
        Request.Builder rb = new Request.Builder().url(request.getUrl()).tag(request.getTag());
        Headers okheader = createHeader(request.getHeaders());
        if(okheader!=null){
            rb.headers(okheader);
        }
        switch (request.getMethod()){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(RequestBodyWrapper.wrapperLite(request.getBody()));
                break;
            case PUT:
                rb = rb.put(RequestBodyWrapper.wrapperLite(request.getBody()));
                break;
            case PATCH:
                rb = rb.patch(RequestBodyWrapper.wrapperLite(request.getBody()));
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(request.getBody()==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(RequestBodyWrapper.wrapperLite(request.getBody()));
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
        Request.Builder rb = createRequestBuilder(request);
        return new ResponseWrapper(mClient.newCall(rb.build()).execute(),request);
    }

    @Override
    public void cancel(Object tag) {
        mClient.cancel(tag);
    }

    @Override
    public RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
                                           List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList){
        MultipartBuilder builder;
        if(boundary==null){
            builder = new MultipartBuilder().type(MediaTypeWrapper.wrapperLite(type));
        }else {
            builder = new MultipartBuilder(boundary).type(MediaTypeWrapper.wrapperLite(type));
        }
        if(bodyList!=null){
            for(RequestBody body:bodyList){
                builder.addPart(RequestBodyWrapper.wrapperLite(body));
            }
        }
        if(headBodyList!=null){
            for(Pair<Map<String,List<String>>,RequestBody> bodyPair:headBodyList){
                builder.addPart(createHeader(bodyPair.first),RequestBodyWrapper.wrapperLite(bodyPair.second));
            }
        }
        if(paramList!=null){
            for(Pair<String,String> pair:paramList){
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if(fileList!=null){
            for(Pair<String,Pair<String,RequestBody>> pair:fileList){
                builder.addFormDataPart(pair.first, pair.second.first, RequestBodyWrapper.wrapperLite(pair.second.second));
            }
        }
        return new RequestBodyWrapper(builder.build());
    }

    public RequestBody createFormBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList){
        FormEncodingBuilder builder = new FormEncodingBuilder();
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
        return new RequestBodyWrapper(builder.build());
    }

    @Override
    public void setConfig(ClientSettings settings) {
        mClient.setProxy(settings.getProxy()).setProxySelector(settings.getProxySelector()).setSocketFactory(settings.getSocketFactory())
                .setSslSocketFactory(settings.getSslSocketFactory())
                .setHostnameVerifier(settings.getHostnameVerifier()).setFollowSslRedirects(settings.isFollowSslRedirects())
                .setFollowRedirects(settings.isFollowRedirects());
        mClient.setRetryOnConnectionFailure(settings.getMaxRetryCount()>0);
        mClient.setConnectTimeout(settings.getConnectTimeout(), TimeUnit.MILLISECONDS);
        mClient.setReadTimeout(settings.getReadTimeout(),TimeUnit.MILLISECONDS);
        mClient.setWriteTimeout(settings.getWriteTimeout(), TimeUnit.MILLISECONDS);
        if(settings.getCookieHandler()!=null)mClient.setCookieHandler(settings.getCookieHandler());
        if(settings.getCacheDir()!=null){
            mClient.setCache(new Cache(settings.getCacheDir(),settings.getCacheMaxSize()));
        }
    }

    public RequestBody createRequestBody(MediaType contentType, String content) {
        com.squareup.okhttp.RequestBody requestBody =
                com.squareup.okhttp.RequestBody.create(MediaTypeWrapper.wrapperLite(contentType),content);
        return new RequestBodyWrapper(requestBody);
    }

    public RequestBody createRequestBody(final MediaType contentType, final byte[] content) {
        return createRequestBody(contentType, content, 0, content.length);
    }

    public RequestBody createRequestBody(final MediaType contentType, final byte[] content,
                                         final int offset, final int byteCount) {
        com.squareup.okhttp.RequestBody requestBody =
                com.squareup.okhttp.RequestBody.create(MediaTypeWrapper.wrapperLite(contentType),content,offset,byteCount);
        return new RequestBodyWrapper(requestBody);
    }

    public RequestBody createRequestBody(final MediaType contentType, final File file) {
        com.squareup.okhttp.RequestBody requestBody =
                com.squareup.okhttp.RequestBody.create(MediaTypeWrapper.wrapperLite(contentType),file);
        return new RequestBodyWrapper(requestBody);
    }

    @Override
    public MediaType parse(String type) {
        com.squareup.okhttp.MediaType oktype = com.squareup.okhttp.MediaType.parse(type);
        return new MediaTypeWrapper(oktype);
    }

    private static Headers createHeader(Map<String, List<String>> headers){
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
