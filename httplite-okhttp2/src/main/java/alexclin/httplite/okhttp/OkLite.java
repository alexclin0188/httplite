package alexclin.httplite.okhttp;

import android.util.Pair;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.ResultCallback;
import alexclin.httplite.okhttp.wrapper.CallWrapper;
import alexclin.httplite.okhttp.wrapper.MediaTypeWrapper;
import alexclin.httplite.okhttp.wrapper.RequestBodyWrapper;
import alexclin.httplite.okhttp.wrapper.ResponseWrapper;

/**
 * alexclin.httplite.okhttp
 *
 * @author alexclin
 * @date 16/1/1 17:16
 */
public class OkLite extends HttpLiteBuilder implements LiteClient{
    public static HttpLiteBuilder create() {
        return create(null);
    }

    public static HttpLiteBuilder create(OkHttpClient client) {
        return new OkLite(client);
    }

    private OkHttpClient mClient;

    OkLite(OkHttpClient client){
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
    public void execute(final String url, final HttpLite.Method method, final Map<String, List<String>> headers, final RequestBody body, final Object tag, final ResultCallback callback, final Runnable preWork) {
        if(preWork!=null){
            mClient.getDispatcher().getExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    preWork.run();
                    executeInternal(url,method,headers,body,tag,callback);
                }
            });
        }else{
            executeInternal(url,method,headers,body,tag,callback);
        }
    }

    private void executeInternal(String url, HttpLite.Method method, Map<String, List<String>> headers, RequestBody body, Object tag, final ResultCallback callback){
        Request.Builder rb = createRequestBuilder(url, method, headers, body, tag);
        new CallWrapper(mClient,rb.build(),callback).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                callback.onFailed(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                callback.onResponse(new ResponseWrapper(response,callback.getCall()));
            }
        });
    }

    private Request.Builder createRequestBuilder(String url, HttpLite.Method method, Map<String, List<String>> headers, RequestBody body, Object tag) {
        Request.Builder rb = new Request.Builder().url(url).tag(tag);
        Headers okheader = createHeader(headers);
        if(okheader!=null){
            rb.headers(okheader);
        }
        switch (method){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(RequestBodyWrapper.wrapperLite(body));
                break;
            case PUT:
                rb = rb.put(RequestBodyWrapper.wrapperLite(body));
                break;
            case PATCH:
                rb = rb.patch(RequestBodyWrapper.wrapperLite(body));
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(body==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(RequestBodyWrapper.wrapperLite(body));
                }
                break;
        }
        return rb;
    }

    @Override
    public alexclin.httplite.Response executeSync(alexclin.httplite.Request request, String url, HttpLite.Method method, Map<String, List<String>> headers, RequestBody body, Object tag) throws IOException{
        Request.Builder rb = createRequestBuilder(url,method,headers,body,tag);
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
                builder.addFormDataPart(pair.first,pair.second);
            }
        }
        if(fileList!=null){
            for(Pair<String,Pair<String,RequestBody>> pair:fileList){
                builder.addFormDataPart(pair.first,pair.second.first,RequestBodyWrapper.wrapperLite(pair.second.second));
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
    public void setConfig(Proxy proxy, ProxySelector proxySelector, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier,
                          boolean followSslRedirects, boolean followRedirects, int maxRetry, int connectTimeout, int readTimeout, int writeTimeout) {
        mClient.setProxy(proxy).setProxySelector(proxySelector).setSocketFactory(socketFactory).setSslSocketFactory(sslSocketFactory)
                .setHostnameVerifier(hostnameVerifier).setFollowSslRedirects(followSslRedirects).setFollowRedirects(followRedirects);
        mClient.setRetryOnConnectionFailure(maxRetry>0);
        mClient.setConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS);
        mClient.setReadTimeout(readTimeout,TimeUnit.MILLISECONDS);
        mClient.setWriteTimeout(writeTimeout,TimeUnit.MILLISECONDS);
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
