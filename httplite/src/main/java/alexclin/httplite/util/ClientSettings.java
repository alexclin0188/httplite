package alexclin.httplite.util;

import java.io.File;
import java.net.CookieHandler;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.concurrent.ExecutorService;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * ClientSettings
 *
 * @author alexclin at 16/2/5 13:30
 */
public final class ClientSettings {
    private Proxy proxy;
    private ProxySelector proxySelector;
    private SocketFactory socketFactory;
    private SSLSocketFactory sslSocketFactory;
    private HostnameVerifier hostnameVerifier;
    private boolean followSslRedirects = true;
    private boolean followRedirects = true;
    private int maxRetryCount = 2;
    private int connectTimeout = 10_000;
    private int readTimeout = 10_000;
    private int writeTimeout = 10_000;
    private CookieHandler cookieHandler;
    private File cacheDir;
    private long cacheMaxSize;
    private ExecutorService executor;

    public ClientSettings() {
    }

    public Proxy getProxy() {
        return proxy;
    }

    public ProxySelector getProxySelector() {
        return proxySelector;
    }

    public SocketFactory getSocketFactory() {
        return socketFactory;
    }

    public SSLSocketFactory getSslSocketFactory() {
        return sslSocketFactory;
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public boolean isFollowSslRedirects() {
        return followSslRedirects;
    }

    public boolean isFollowRedirects() {
        return followRedirects;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getWriteTimeout() {
        return writeTimeout;
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public CookieHandler getCookieHandler() {
        return cookieHandler;
    }

    public long getCacheMaxSize() {
        return cacheMaxSize;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setProxySelector(ProxySelector proxySelector) {
        this.proxySelector = proxySelector;
    }

    public void setSocketFactory(SocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }

    public void setSslSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public void setFollowSslRedirects(boolean followSslRedirects) {
        this.followSslRedirects = followSslRedirects;
    }

    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public void setCookieHandler(CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public void setCacheMaxSize(long cacheMaxSize) {
        this.cacheMaxSize = cacheMaxSize;
    }

    public void setExecutorService(ExecutorService executor){
        this.executor = executor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }
}
