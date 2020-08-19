package alexclin.httplite.okhttp3;

import java.io.IOException;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

/**
 * CookieJarImpl
 *
 * @author alexclin 16/2/16 20:54
 */
class CookieJarImpl implements CookieJar {
    public static final String COOKIE = "Cookie";
    private CookieHandler cookieHandler;

    CookieJarImpl(CookieHandler cookieHandler) {
        this.cookieHandler = cookieHandler;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        try {
            Map<String, List<String>> map = cookiesToMap(cookies);
            if(!map.isEmpty())cookieHandler.put(url.uri(),map);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        try {
            Map<String, List<String>> map = cookieHandler.get(url.uri(),new HashMap<String, List<String>>(0));
            return mapToCookies(url,map);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Cookie> mapToCookies(HttpUrl url,Map<String, List<String>> map) {
        List<String> list = map.get(COOKIE);
        List<Cookie> cookies = new ArrayList<>();
        if(list!=null&&list.size()>0){
            for(String cookie:list){
                Cookie c = Cookie.parse(url,cookie);
                cookies.add(c);
            }
        }
        return cookies;
    }

    private Map<String, List<String>> cookiesToMap(List<Cookie> cookies) {
        List<String> list = new ArrayList<>();
        for(Cookie cookie:cookies){
            list.add(cookie.toString());
        }
        Map<String, List<String>> map = new HashMap<>();
        map.put(COOKIE,list);
        return map;
    }


}
