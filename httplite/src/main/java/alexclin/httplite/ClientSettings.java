package alexclin.httplite;

import java.io.File;
import java.net.CookieHandler;
import java.net.Proxy;
import java.net.ProxySelector;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;

/**
 * ClientSettings
 *
 * @author alexclin at 16/2/5 13:30
 */
public final class ClientSettings {
    Proxy proxy;
    ProxySelector proxySelector;
    SocketFactory socketFactory;
    SSLSocketFactory sslSocketFactory;
    HostnameVerifier hostnameVerifier;
    boolean followSslRedirects = true;
    boolean followRedirects = true;
    int maxRetryCount = 2;
    int connectTimeout = 10_000;
    int readTimeout = 10_000;
    int writeTimeout = 10_000;
    CookieHandler cookieHandler;
    File cacheDir;
    long cacheMaxSize;

    ClientSettings() {
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
}
