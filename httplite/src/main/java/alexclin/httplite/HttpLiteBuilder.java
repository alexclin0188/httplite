package alexclin.httplite;

import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.mock.MockCall;
import alexclin.httplite.listener.RequestFilter;
import alexclin.httplite.listener.ResponseFilter;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.retrofit.CallAdapter;
import alexclin.httplite.util.ClientSettings;

/**
 * HttpLiteBuilder
 *
 * @author alexclin at 16/1/1 10:56
 */
public abstract class HttpLiteBuilder{
    private String baseUrl;
    private boolean isRelease;
    private RequestFilter mRequestFilter;
    private ResponseFilter mResponseFilter;
    private Executor downloadExecutor;
    private List<CallAdapter> invokers;

    private ClientSettings settings = new ClientSettings();

    private HashMap<String,ResponseParser> parserMap;

    protected abstract LiteClient initLiteClient();

    public final HttpLite build(){
        LiteClient client = initLiteClient();
        client.setConfig(settings);
        return new HttpLite(client,baseUrl,settings.getMaxRetryCount(),new HttpCall.Factory(), isRelease,
                mRequestFilter,mResponseFilter, downloadExecutor,parserMap,invokers);
    }

    public final HttpLite mock(MockHandler mockHandler){
        LiteClient client = initLiteClient();
        client.setConfig(settings);
        return new HttpLite(client,baseUrl,settings.getMaxRetryCount(),new MockCall.MockFactory(mockHandler), isRelease,
                mRequestFilter,mResponseFilter, downloadExecutor,parserMap,invokers);
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
        settings.setConnectTimeout((int) millis);
        return this;
    }

    public HttpLiteBuilder setReadTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
        if (unit == null) throw new IllegalArgumentException("unit == null");
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
        if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
        settings.setReadTimeout((int) millis);
        return this;
    }

    public HttpLiteBuilder setWriteTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
        if (unit == null) throw new IllegalArgumentException("unit == null");
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
        if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
        settings.setWriteTimeout((int) millis);
        return this;
    }

    public HttpLiteBuilder setProxy(Proxy proxy) {
        settings.setProxy(proxy);
        return this;
    }

    public HttpLiteBuilder setProxySelector(ProxySelector proxySelector) {
        settings.setProxySelector(proxySelector);
        return this;
    }

    public HttpLiteBuilder setSocketFactory(SocketFactory socketFactory) {
        settings.setSocketFactory(socketFactory);
        return this;
    }


    public HttpLiteBuilder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        settings.setSslSocketFactory(sslSocketFactory);
        return this;
    }

    public HttpLiteBuilder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        settings.setHostnameVerifier(hostnameVerifier);
        return this;
    }

    public HttpLiteBuilder setFollowSslRedirects(boolean followProtocolRedirects) {
        settings.setFollowSslRedirects(followProtocolRedirects);
        return this;
    }

    public HttpLiteBuilder setFollowRedirects(boolean followRedirects) {
        settings.setFollowRedirects(followRedirects);
        return this;
    }

    public HttpLiteBuilder setMaxRetryCount(int maxRetryCount) {
        settings.setMaxRetryCount(maxRetryCount<1?1:maxRetryCount);
        return this;
    }

    public HttpLiteBuilder useCookie(CookieStore cookieStore){
        settings.setCookieHandler(new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL));
        return this;
    }

    public HttpLiteBuilder useCookie(CookieStore cookieStore,CookiePolicy policy){
        settings.setCookieHandler(new CookieManager(cookieStore, policy));
        return this;
    }

    public HttpLiteBuilder setRelease(boolean isRelase){
        this.isRelease = isRelase;
        return this;
    }

    public HttpLiteBuilder setCache(File dir,long maxCacheSize){
        settings.setCacheDir(dir);
        settings.setCacheMaxSize(maxCacheSize);
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

    public HttpLiteBuilder customDownloadExecutor(ExecutorService executor){
        this.downloadExecutor = executor;
        return this;
    }

    public HttpLiteBuilder addRetrofitInvoker(CallAdapter invoker){
        if(invokers==null) invokers = new ArrayList<>();
        if(invoker!=null&&!invokers.contains(invoker)){
            this.invokers.add(invoker);
        }
        return this;
    }
}
