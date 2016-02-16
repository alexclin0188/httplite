package alexclin.httplite;

import java.io.File;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.concurrent.TimeUnit;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 10:56
 */
public abstract class HttpLiteBuilder{
    private String baseUrl;
    private boolean useLiteRetry;
    private boolean isRelase;
    protected CallFactory callFactory;

    private ClientSettings settings = new ClientSettings();

    protected abstract LiteClient initLiteClient();

    public final HttpLite build(){
        LiteClient client = initLiteClient();
        if(callFactory==null) callFactory = new HttpCall.Factory();
        settings.maxRetryCount = useLiteRetry?0:settings.maxRetryCount;
        client.setConfig(settings);
        return new HttpLite(client,baseUrl,useLiteRetry,settings.maxRetryCount,callFactory,isRelase);
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

    /**
     * Sets the default write timeout for new connections. A name of 0 means no timeout, otherwise
     * values must be between 1 and {@link Integer#MAX_VALUE} when converted to milliseconds.
     */
    public HttpLiteBuilder setWriteTimeout(long timeout, TimeUnit unit) {
        if (timeout < 0) throw new IllegalArgumentException("timeout < 0");
        if (unit == null) throw new IllegalArgumentException("unit == null");
        long millis = unit.toMillis(timeout);
        if (millis > Integer.MAX_VALUE) throw new IllegalArgumentException("Timeout too large.");
        if (millis == 0 && timeout > 0) throw new IllegalArgumentException("Timeout too small.");
        settings.writeTimeout = (int) millis;
        return this;
    }

    /**
     * Sets the HTTP proxy that will be used by connections created by this client. This takes
     * precedence over {@link #setProxySelector}, which is only honored when this proxy is null (which
     * it is by default). To disable proxy use completely, call {@code setProxy(Proxy.NO_PROXY)}.
     */
    public HttpLiteBuilder setProxy(Proxy proxy) {
        settings.proxy = proxy;
        return this;
    }

    /**
     * Sets the proxy selection policy to be used if no {@link #setProxy proxy} is specified
     * explicitly. The proxy selector may return multiple proxies; in that case they will be tried in
     * sequence until a successful connection is established.
     *
     * <p>If unset, the {@link ProxySelector#getDefault() system-wide default} proxy selector will be
     * used.
     */
    public HttpLiteBuilder setProxySelector(ProxySelector proxySelector) {
        settings.proxySelector = proxySelector;
        return this;
    }

    /**
     * Sets the socket factory used to create connections. OkHttp only uses the parameterless {@link
     * SocketFactory#createSocket() createSocket()} method to create unconnected sockets. Overriding
     * this method, e. g., allows the socket to be bound to a specific local address.
     *
     * <p>If unset, the {@link SocketFactory#getDefault() system-wide default} socket factory will be
     * used.
     */
    public HttpLiteBuilder setSocketFactory(SocketFactory socketFactory) {
        settings.socketFactory = socketFactory;
        return this;
    }

    /**
     * Sets the socket factory used to secure HTTPS connections.
     *
     * <p>If unset, a lazily created SSL socket factory will be used.
     */
    public HttpLiteBuilder setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        settings.sslSocketFactory = sslSocketFactory;
        return this;
    }

    /**
     * Sets the verifier used to confirm that response certificates apply to requested hostnames for
     * HTTPS connections.
     *
     * <p>If unset, a default hostname verifier will be used.
     */
    public HttpLiteBuilder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        settings.hostnameVerifier = hostnameVerifier;
        return this;
    }

    /**
     * Configure this client to follow redirects from HTTPS to HTTP and from HTTP to HTTPS.
     *
     * <p>If unset, protocol redirects will be followed. This is different than the built-in {@code
     * HttpURLConnection}'s default.
     */
    public HttpLiteBuilder setFollowSslRedirects(boolean followProtocolRedirects) {
        settings.followSslRedirects = followProtocolRedirects;
        return this;
    }

    /** Configure this client to follow redirects. If unset, redirects be followed. */
    public HttpLiteBuilder setFollowRedirects(boolean followRedirects) {
        settings.followRedirects = followRedirects;
        return this;
    }

    public HttpLiteBuilder setMaxRetryCount(int maxRetryCount) {
        settings.maxRetryCount = maxRetryCount<1?1:maxRetryCount;
        return this;
    }

    public HttpLiteBuilder useLiteRetry(boolean useLiteRetry){
        this.useLiteRetry = useLiteRetry;
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
}
