package alexclin.httplite.url;

import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.url.cache.CacheHandler;
import alexclin.httplite.util.ClientSettings;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.listener.Response;
import alexclin.httplite.listener.ResponseBody;
import alexclin.httplite.impl.ResponseImpl.ResponseBodyImpl;
import alexclin.httplite.impl.ResponseImpl;
import alexclin.httplite.url.cache.CacheImpl;
import alexclin.httplite.util.LogUtil;

/**
 * URLite
 *
 * @author alexclin 16/1/1 20:53
 */
public class URLite implements LiteClient {
    ClientSettings settings;
    private NetDispatcher mNetDispatcher;
    private CacheDispatcher mCacheDispatcher;
    private CacheImpl mCache;
    private CacheHandler mCachePolicy;
    private CookieHandler mCookieHandler;

    private URLiteFactory mFactory;

    private URLite(ClientSettings settings) {
        mNetDispatcher = new NetDispatcher(this,settings.getRequestExecutor());
        mFactory = new URLiteFactory();
    }

    static Response createResponse(HttpURLConnection urlConnection, Request request) throws IOException {
        ResponseBody body = createResponseBody(urlConnection);
        return new ResponseImpl(request, urlConnection.getResponseCode(), urlConnection.getResponseMessage(),
                urlConnection.getHeaderFields(), body);
    }

    public static Response createResponse(int code, String message, Map<String, List<String>> headers, String mediaType, long length, InputStream inputStream, Request request) {
        return new ResponseImpl(request, code, message, headers, new ResponseBodyImpl(inputStream, URLMediaType.parse(mediaType), length));
    }

    private static ResponseBody createResponseBody(HttpURLConnection urlConnection) throws IOException {
        long contentLength = urlConnection.getContentLength();
        MediaType type = URLMediaType.parse(urlConnection.getContentType());
        InputStream stream;
        try {
            stream = urlConnection.getInputStream();
        } catch (IOException ioe) {
            stream = urlConnection.getErrorStream();
        }
        return new ResponseBodyImpl(stream, type, contentLength);
    }

    RequestBody realBody(RequestBody requestBody){
        return mFactory.createRequestBody(requestBody,null);
    }

    @Override
    public void shutDown() {
        cancelAll();
        mCacheDispatcher.shutdown();
        mNetDispatcher.shutdown();
    }

    @Override
    public MediaType mediaType(String mediaType) {
        return URLMediaType.parse(mediaType);
    }

    @Override
    public Response execute(Request request) throws Exception {
        URLTask task = new URLTask(request,null);
        request.handle().setHandle(task);
        return dispatchTaskSync(task);
    }

    @Override
    public void enqueue(Request request, Callback<Response> callback) {
        URLTask task = new URLTask(request,callback);
        request.handle().setHandle(task);
        dispatchTask(task);
    }

    @Override
    public void cancel(Object tag) {
        if (mCacheDispatcher != null)
            mCacheDispatcher.cancel(tag);
        mNetDispatcher.cancel(tag);
    }

    @Override
    public void cancelAll() {
        if (mCacheDispatcher != null)
            mCacheDispatcher.cancelAll();
        mNetDispatcher.cancelAll();
    }

    void processCookie(String url, Map<String, List<String>> headers) {
        if (!isUseCookie()) {
            return;
        }
        try {
            CookieHandler cookieManager = getCookieHandler();
            Map<String, List<String>> singleMap =
                    cookieManager.get(URI.create(url), new HashMap<String, List<String>>(0));
            List<String> cookies = singleMap.get("Cookie");
            if (cookies != null) {
                headers.put("Cookie", Collections.singletonList(TextUtils.join(";", cookies)));
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    void saveCookie(String url, Map<String, List<String>> headers) {
        if (!isUseCookie()) {
            return;
        }
        try {
            CookieHandler cookieManager = getCookieHandler();
            if (headers != null) {
                cookieManager.put(URI.create(url), headers);
            }
        } catch (Throwable ex) {
            LogUtil.e(ex.getMessage(), ex);
        }
    }

    private boolean isUseCookie() {
        return mCookieHandler != null;
    }

    private CookieHandler getCookieHandler() {
        return mCookieHandler;
    }

    private void dispatchTask(Task task) {
        if (isCacheAble(task)) {
            mCacheDispatcher.dispatch(task);
        } else {
            mNetDispatcher.dispatch(task);
        }
    }

    Response dispatchTaskSync(Task task) throws Exception {
        if (isCacheAble(task))
            return mCacheDispatcher.execute(task);
        else
            return mNetDispatcher.execute(task);
    }

    boolean isCacheAble(Task task) {
        return mCacheDispatcher!=null&&mCacheDispatcher.canCache(task.request());
    }

    boolean hasCacheSetting(){
        return mCacheDispatcher!=null;
    }

    Response createCacheResponse(Response response) throws IOException {
        if (mCacheDispatcher != null) {
            return mCacheDispatcher.cacheResponse(response);
        }else {
            return response;
        }
    }

    void addCacheHeaders(Request request,Map<String, List<String>> headers) {
        if (mCacheDispatcher != null) mCacheDispatcher.addCacheHeaders(request,headers);
    }

    public static Builder builder(){
        return new Builder();
    }

    public static class Builder extends HttpLiteBuilder{
        private CacheHandler cachePolicy;
        private CookieHandler cookieHandler;
        private int maxRequests;

        private Builder() {
        }

        @Override
        protected LiteClient initClient(ClientSettings settings) {
            URLite lite = new URLite(settings);
            lite.settings = settings;
            lite.mCookieHandler = cookieHandler;
            if(maxRequests>5){
                lite.mNetDispatcher.setMaxRequests(maxRequests);
            }
            if (settings.getCacheDir() != null) {
                if(cachePolicy==null)
                    lite.mCachePolicy = new CacheDispatcher.DefaultCachePolicy();
                else
                    lite.mCachePolicy = cachePolicy;
                try {
                    lite.mCache = new CacheImpl(settings.getCacheDir(), settings.getCacheMaxSize(),lite.mCachePolicy);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (lite.mCache != null)
                    lite.mCacheDispatcher = new CacheDispatcher(lite.mNetDispatcher, lite.mCache);
            }
            return lite;
        }

        public Builder setCacheHandler(CacheHandler cachePolicy){
            this.cachePolicy = cachePolicy;
            return this;
        }

        public Builder setCookieStore(CookieStore cookieStore){
            cookieHandler = new CookieManager(cookieStore, CookiePolicy.ACCEPT_ALL);
            return this;
        }

        public Builder setCookieStore(CookieStore cookieStore, CookiePolicy policy){
            cookieHandler = new CookieManager(cookieStore, policy);
            return this;
        }

        public Builder setMaxRequests(int maxRequests){
            this.maxRequests = maxRequests;
            return this;
        }
    }
}
