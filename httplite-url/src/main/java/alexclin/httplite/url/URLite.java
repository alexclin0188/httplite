package alexclin.httplite.url;

import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.ClientSettings;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.Response;
import alexclin.httplite.ResultCallback;
import alexclin.httplite.url.cache.CacheImpl;
import alexclin.httplite.util.LogUtil;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/1 20:53
 */
public class URLite extends HttpLiteBuilder implements LiteClient {
    ClientSettings settings;

    private NetworkDispatcher mNetDispatcher;
    private CacheDispatcher mCacheDispatcher;
    private URLCache mCache;

    public static HttpLiteBuilder create() {
        return new URLite();
    }

    public URLite() {
        mNetDispatcher = new NetworkDispatcher();
    }

    @Override
    public void setConfig(ClientSettings settings) {
        this.settings = settings;
        mNetDispatcher.setMaxRequests(settings.getMaxRetryCount());
        if(settings.getCacheDir()!=null){
            try {
                mCache = new CacheImpl(settings.getCacheDir(),settings.getCacheMaxSize());
            } catch (IOException e) {
                e.printStackTrace();
            }
            if(mCache!=null) mCacheDispatcher = new CacheDispatcher(mNetDispatcher,mCache);
        }
    }

    @Override
    public Handle execute(Request request, ResultCallback callback, Runnable preWork) {
        LogUtil.e("Before Execute url:" + request.getUrl());
        URLTask task = new URLTask(this, request, callback, preWork);
        dispatchTask(task);
        return task;
    }

    @Override
    public Response executeSync(Request request) throws Exception {
        URLTask task = new URLTask(this, request, null, null);
        return dispatchTaskSync(task);
    }

    @Override
    public void cancel(Object tag) {
        mCacheDispatcher.cancel(tag);
        mNetDispatcher.cancel(tag);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, String content) {
        return URLRequestBody.create(contentType, content);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, byte[] content) {
        return URLRequestBody.create(contentType, content);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, byte[] content, int offset, int byteCount) {
        return URLRequestBody.create(contentType, content, offset, byteCount);
    }

    @Override
    public RequestBody createRequestBody(MediaType contentType, File file) {
        return URLRequestBody.create(contentType, file);
    }

    @Override
    public MediaType parse(String type) {
        return URLMediaType.parse(type);
    }

    @Override
    public RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String, List<String>>, RequestBody>> headBodyList, List<Pair<String, String>> paramList, List<Pair<String, Pair<String, RequestBody>>> fileList) {
        URLMultipartBody.Builder builder;
        if (boundary == null) {
            builder = new URLMultipartBody.Builder().setType(type);
        } else {
            builder = new URLMultipartBody.Builder(boundary).setType(type);
        }
        if (bodyList != null) {
            for (RequestBody body : bodyList) {
                builder.addPart(body);
            }
        }
        if (headBodyList != null) {
            for (Pair<Map<String, List<String>>, RequestBody> bodyPair : headBodyList) {
                builder.addPart(bodyPair.first, bodyPair.second);
            }
        }
        if (paramList != null) {
            for (Pair<String, String> pair : paramList) {
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if (fileList != null) {
            for (Pair<String, Pair<String, RequestBody>> pair : fileList) {
                builder.addFormDataPart(pair.first, pair.second.first, pair.second.second);
            }
        }
        return builder.build();
    }

    @Override
    public RequestBody createFormBody(List<Pair<String, String>> paramList, List<Pair<String, String>> encodedParamList) {
        URLFormBody.Builder builder = new URLFormBody.Builder();
        if (paramList != null) {
            for (Pair<String, String> param : paramList) {
                builder.add(param.first, param.second);
            }
        }
        if (encodedParamList != null) {
            for (Pair<String, String> param : encodedParamList) {
                builder.addEncoded(param.first, param.second);
            }
        }
        return builder.build();
    }

    @Override
    protected LiteClient initLiteClient() {
        return this;
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
        return settings.getCookieHandler() != null;
    }

    private CookieHandler getCookieHandler() {
        return settings.getCookieHandler();
    }

    private void dispatchTask(Task task) {
        if (task.request().canCache()) {
            mCacheDispatcher.dispatch(task);
        } else {
            mNetDispatcher.dispatch(task);
        }
    }

    private Response dispatchTaskSync(Task task) throws Exception {
        if (task.request().canCache())
            return mCacheDispatcher.execute(task);
        else
            return mNetDispatcher.execute(task);
    }

    boolean isCacheAble(Task task) {
        return task.request().canCache() && mCacheDispatcher != null;
    }

    public Response createCacheResponse(Response response) throws IOException{
        if (mCacheDispatcher != null)
            return mCacheDispatcher.cacheResponse(response);
        else
            return response;
    }

    public void addCacheHeaders(Request request){
        if(mCacheDispatcher!=null) mCacheDispatcher.addCacheHeaders(request);
    }

    public NetworkDispatcher getNetDispatcher() {
        return mNetDispatcher;
    }
}
