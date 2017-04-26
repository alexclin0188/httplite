package alexclin.httplite.okhttp3;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.CookieHandler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
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
            return mapToCookies(map);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private List<Cookie> mapToCookies(Map<String, List<String>> map) {
        List<Cookie> cookieList = new ArrayList<>();
        if(map==null||map.isEmpty()) return cookieList;
        for(List<String> list:map.values()){
            if(list.size()>0){
                Cookie cookie = fromJson(list.get(0));
                if(cookie!=null) cookieList.add(cookie);
            }
        }
        return cookieList;
    }

    private Map<String, List<String>> cookiesToMap(List<Cookie> cookies) {
        Map<String, List<String>> map = new IdentityHashMap<>();
        if(cookies==null||cookies.isEmpty()) return map;
        for(Cookie cookie:cookies){
            String json = toJson(cookie);
            if(json!=null){
                map.put(cookie.name(), Collections.singletonList(json));
            }
        }
        return map;
    }

    private Cookie fromJson(String json){
        try {
            JSONObject jsonObject = new JSONObject(json);
            Cookie.Builder builder = new Cookie.Builder();
            builder.name(jsonObject.getString("name"));
            builder.value(jsonObject.getString("value"));
            builder.domain(jsonObject.getString("domain"));
            builder.path(jsonObject.getString("path"));
            builder.expiresAt(jsonObject.getLong("expiresAt"));
            if(jsonObject.getBoolean("secure")) builder.secure();
            if(jsonObject.getBoolean("httpOnly")) builder.httpOnly();
            return builder.build();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String toJson(Cookie cookie){
        try {
            JSONObject jobj = new JSONObject();
            jobj.put("name",cookie.name());
            jobj.put("value",cookie.value());
            jobj.put("domain",cookie.domain());
            jobj.put("path",cookie.path());
            jobj.put("expiresAt",cookie.expiresAt());
            jobj.put("secure",cookie.secure());
            jobj.put("httpOnly",cookie.httpOnly());
            return jobj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
