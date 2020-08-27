package alexclin.httplite;

import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.listener.RequestInterceptor;
import alexclin.httplite.listener.ResponseParser;
import alexclin.httplite.util.ClientSettings;
import alexclin.httplite.util.Util;

/**
 * HttpLiteBuilder
 *
 * @author alexclin at 16/1/1 10:56
 */
public abstract class HttpLiteBuilder{
    String baseUrl;
    RequestInterceptor mRequestInterceptor;
    MockHandler mockHandler;
    private ClientSettings settings = new ClientSettings();
    List<ResponseParser> parsers = new ArrayList<>();
    ExecutorService mockExecutor;

    protected abstract LiteClient initClient(ClientSettings settings);

    public final HttpLite build(){
        LiteClient client = initClient(settings);
        return new HttpLite(this,client);
    }

    public HttpLiteBuilder setBaseUrl(String baseUrl){
        if(!Util.isHttpPrefix(baseUrl)){
            throw new IllegalArgumentException("You must set a setBaseUrl start with http/https prefix for global BaseUrl");
        }
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
        settings.setMaxRetryCount(Math.max(maxRetryCount, 1));
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
        parsers.add(parser);
        return this;
    }

    public HttpLiteBuilder setMockHandler(MockHandler mockHandler){
        this.mockHandler = mockHandler;
        return this;
    }

    public HttpLiteBuilder setMockExecutor(ExecutorService executor){
        this.mockExecutor = executor;
        return this;
    }

    public HttpLiteBuilder setRequestExecutor(ExecutorService executor){
        this.settings.setRequestExecutor(executor);
        return this;
    }

    public HttpLiteBuilder setRequestInterceptor(RequestInterceptor requestFilter){
        this.mRequestInterceptor = requestFilter;
        return this;
    }
}
