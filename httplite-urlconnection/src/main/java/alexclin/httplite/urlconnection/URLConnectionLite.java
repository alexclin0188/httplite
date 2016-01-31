package alexclin.httplite.urlconnection;

import android.util.Pair;

import java.io.File;
import java.net.Proxy;
import java.net.ProxySelector;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.Response;
import alexclin.httplite.ResultCallback;
import alexclin.httplite.urlconnection.util.OkHostnameVerifier;
import alexclin.httplite.util.LogUtil;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/1 20:53
 */
public class URLConnectionLite extends HttpLiteBuilder implements LiteClient {
    Proxy proxy;
    ProxySelector proxySelector;
    SocketFactory socketFactory;
    SSLSocketFactory sslSocketFactory;
    HostnameVerifier hostnameVerifier;
    boolean followSslRedirects;
    boolean followRedirects;
    int maxRetry;
    int connectTimeout;
    int readTimeout;
    int writeTimeout;

    private Dispatcther mDispatcher;

    public static HttpLiteBuilder create() {
        return new URLConnectionLite();
    }

    public URLConnectionLite() {
        mDispatcher = new Dispatcther();
    }

    public Dispatcther getDispatcher() {
        return mDispatcher;
    }

    @Override
    public void setConfig(Proxy proxy, ProxySelector proxySelector, SocketFactory socketFactory, SSLSocketFactory sslSocketFactory, HostnameVerifier hostnameVerifier,
                          boolean followSslRedirects, boolean followRedirects, int maxRetry, int connectTimeout, int readTimeout, int writeTimeout) {
        this.proxy = proxy;
        this.proxySelector = proxySelector;
        this.socketFactory = socketFactory;
        this.hostnameVerifier = hostnameVerifier == null ? getDefaultHostnameVerifier() : hostnameVerifier;
        this.sslSocketFactory = sslSocketFactory == null ? getDefaultSSLSocketFactory() : sslSocketFactory;
        this.followSslRedirects = followSslRedirects;
        this.followRedirects = followRedirects;
        this.maxRetry = maxRetry;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
    }

    @Override
    public void execute(String url, HttpLite.Method method, Map<String, List<String>> headers, RequestBody body, Object tag, ResultCallback callback, Runnable preWork) {
        LogUtil.e("Before Execute url:"+url);
        URLTask task = new URLTask(this, url, method, headers, body, tag,callback.getCall(),callback, preWork);
        getDispatcher().dispatch(task);
    }

    @Override
    public Response executeSync(Request request, String url, HttpLite.Method method, Map<String, List<String>> headers, RequestBody body, Object tag) throws Exception {
        URLTask task = new URLTask(this, url, method, headers, body, tag, request,null,null);
        return getDispatcher().execute(task);
    }

    @Override
    public void cancel(Object tag) {
        mDispatcher.cancel(tag);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, String content) {
        return URLRequestBody.create(contentType, content);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, byte[] content) {
        return URLRequestBody.create(contentType, content);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, byte[] content, int offset, int byteCount) {
        return URLRequestBody.create(contentType, content, offset, byteCount);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, File file) {
        return URLRequestBody.create(contentType, file);
    }

    @Override
    public MediaType parse(String type) {
        return URLMediaType.parse(type);
    }

    @Override
    public RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String, List<String>>, RequestBody>> headBodyList, List<Pair<String, String>> paramList, List<Pair<String, Pair<String, RequestBody>>> fileList) {
        URLMultipartBody.Builder builder;
        if (boundary == null) {
            builder = new URLMultipartBody.Builder().setType(type);
        } else {
            builder = new URLMultipartBody.Builder(boundary).setType(type);
        }
        if (bodyList != null) {
            for (RequestBody body : bodyList) {
                builder.addPart(body);
            }
        }
        if (headBodyList != null) {
            for (Pair<Map<String, List<String>>, RequestBody> bodyPair : headBodyList) {
                builder.addPart(bodyPair.first, bodyPair.second);
            }
        }
        if (paramList != null) {
            for (Pair<String, String> pair : paramList) {
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if (fileList != null) {
            for (Pair<String, Pair<String, RequestBody>> pair : fileList) {
                builder.addFormDataPart(pair.first, pair.second.first, pair.second.second);
            }
        }
        return builder.build();
    }

    @Override
    public RequestBody createFormBody(List<Pair<String, String>> paramList, List<Pair<String, String>> encodedParamList) {
        URLFormBody.Builder builder = new URLFormBody.Builder();
        if (paramList != null) {
            for (Pair<String, String> param : paramList) {
                builder.add(param.first, param.second);
            }
        }
        if (encodedParamList != null) {
            for (Pair<String, String> param : encodedParamList) {
                builder.addEncoded(param.first, param.second);
            }
        }
        return builder.build();
    }

    @Override
    protected LiteClient initLiteClient() {
        return this;
    }

    public static SSLSocketFactory getDefaultSSLSocketFactory() {
        //TODO
        // 信任所有证书
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, null);
            SSLSocketFactory factory = sslContext.getSocketFactory();
            return factory;
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
        return null;
    }

    public HostnameVerifier getDefaultHostnameVerifier() {
        return OkHostnameVerifier.INSTANCE;
    }
}
