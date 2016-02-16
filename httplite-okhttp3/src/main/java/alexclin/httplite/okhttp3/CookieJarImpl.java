package alexclin.httplite.okhttp3;

import java.net.CookieHandler;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * CookieJarImpl
 *
 * @author alexclin
 * @date 16/2/16 20:54
 */
public class CookieJarImpl implements CookieJar {
    private CookieHandler cookieHandler;

    public CookieJarImpl(CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        //TODO
        //cookieHandler.put(url.uri(),);
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        //TODO
        return null;
    }
}
