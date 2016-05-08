package alexclin.httplite.okhttp3;

import android.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.Executable;
import alexclin.httplite.Request;
import alexclin.httplite.util.ClientSettings;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.util.Util;
import okhttp3.Cache;
import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;

/**
 * Ok3Lite
 *
 * @author alexclin 16/2/16 20:15
 */
public class Ok3Lite extends HttpLiteBuilder implements LiteClient{
    private OkHttpClient mClient;

    Ok3Lite(OkHttpClient client){
        if(client==null){
            mClient = new OkHttpClient();
        }else{
            mClient = client;
        }
    }

    public static HttpLiteBuilder create() {
        return create(null);
    }

    public static HttpLiteBuilder create(OkHttpClient client) {
        return new Ok3Lite(client);
    }

    @Override
    protected LiteClient initLiteClient() {
        return this;
    }

    public Executable executable(Request request){
        return new OkTask(request,mClient);
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
                builder.addPart(OkTask.createHeader(bodyPair.first), OkRequestBody.wrapperLite(bodyPair.second));
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
}
