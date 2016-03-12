package alexclin.httplite;

import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.internal.MockCall;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.listener.ResponseFilter;
import alexclin.httplite.listener.ResponseParser;

/**
 * HttpLiteBuilder
 *
 * @author alexclin at 16/1/1 10:56
 */
public abstract class HttpLiteBuilder{
    private String baseUrl;
    private boolean isRelase;
    private RequestFilter mRequestFilter;
    private ResponseFilter mResponseFilter;
    private Executor customDownloadExecutor;

    private ClientSettings settings = new ClientSettings();

    private HashMap<String,ResponseParser> parserMap;

    protected abstract LiteClient initLiteClient();

    public final HttpLite build(){
        LiteClient client = initLiteClient();
        client.setConfig(settings);
        return new HttpLite(client,baseUrl,settings.maxRetryCount,new HttpCall.Factory(),isRelase,
                mRequestFilter,mResponseFilter,customDownloadExecutor);
    }

    public final HttpLite mock(MockHandler mockHandler){
        LiteClient client = initLiteClient();
        client.setConfig(settings);
        return new HttpLite(client,baseUrl,settings.maxRetryCount,new MockCall.MockFactory(mockHandler),isRelase,
                mRequestFilter,mResponseFilter,customDownloadExecutor);
    }

    public HttpLiteBuilder baseUrl(String baseUrl){
        this.baseUrl = baseUrl;
        return this;
    }

    public HttpLiteBuilder setConnectTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
        if (unit == null) throw new IllegalArgumentException("unit == null");
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
        if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
        settings.connectTimeout = (int) millis;
        return this;
    }

    public HttpLiteBuilder setReadTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
        if (unit == null) throw new IllegalArgumentException("unit == null");
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
        if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
        settings.readTimeout = (int) millis;
        return this;
    }

    public HttpLiteBuilder setWriteTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
        if (unit == null) throw new IllegalArgumentException("unit == null");
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
        if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
        settings.writeTimeout = (int) millis;
        return this;
    }

    public HttpLiteBuilder setProxy(Proxy proxy) {
        settings.proxy = proxy;
        return this;
    }

    public HttpLiteBuilder setProxySelector(ProxySelector proxySelector) {
        settings.proxySelector = proxySelector;
        return this;
    }

    public HttpLiteBuilder setSocketFactory(SocketFactory socketFactory) {
        settings.socketFactory = socketFactory;
        return this;
    }


    public HttpLiteBuilder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        settings.sslSocketFactory = sslSocketFactory;
        return this;
    }

    public HttpLiteBuilder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        settings.hostnameVerifier = hostnameVerifier;
        return this;
    }

    public HttpLiteBuilder setFollowSslRedirects(boolean followProtocolRedirects) {
        settings.followSslRedirects = followProtocolRedirects;
        return this;
    }

    public HttpLiteBuilder setFollowRedirects(boolean followRedirects) {
        settings.followRedirects = followRedirects;
        return this;
    }

    public HttpLiteBuilder setMaxRetryCount(int maxRetryCount) {
        settings.maxRetryCount = maxRetryCount<1?1:maxRetryCount;
        return this;
    }

    public HttpLiteBuilder useCookie(CookieStore cookieStore){
        settings.cookieHandler = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
        return this;
    }

    public HttpLiteBuilder useCookie(CookieStore cookieStore,CookiePolicy policy){
        settings.cookieHandler = new CookieManager(cookieStore, policy);
        return this;
    }

    public HttpLiteBuilder setRelease(boolean isRelase){
        this.isRelase = isRelase;
        return this;
    }

    public HttpLiteBuilder setCache(File dir,long maxCacheSize){
        settings.cacheDir = dir;
        settings.cacheMaxSize = maxCacheSize;
        return this;
    }

    public HttpLiteBuilder addResponseParser(ResponseParser parser){
        if(null==parser){
            return this;
        }
        if(parserMap==null) parserMap = new HashMap<>();
        String key = parser.getClass().getName();
        parserMap.put(key,parser);
        return this;
    }

    public HttpLiteBuilder requestFilter(RequestFilter requestFilter){
        this.mRequestFilter = requestFilter;
        return this;
    }

    public HttpLiteBuilder responseFilter(ResponseFilter responseFilter){
        this.mResponseFilter = responseFilter;
        return this;
    }
}
