package alexclin.httplite.url;

import android.os.Build;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.exception.IllegalOperationException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.Response;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.util.LogUtil;

/**
 * URLTask
 *
 * @author alexclin 16/1/2 19:39
 */
class URLTask implements Task,Comparable<Task>,Handle{

    private Request request;
    private int retryCount;

    private Callback<Response> callback;

    private volatile boolean isExecuted;
    private volatile boolean isCanceled;

    URLTask(Request request,Callback<Response> callback) {
        this.request = request;
        this.callback = callback;
    }

    public void enqueue(URLite lite) {
        int maxRetry = lite.settings.getMaxRetryCount();
        Response response = null;
        while (retryCount<= maxRetry&& !isCanceled()){
            try {
                retryCount++;
                response = lite.dispatchTaskSync(this);
                if(response!=null){
                    break;
                }
            }catch (Exception e) {
                e.printStackTrace();
                if(retryCount>maxRetry || e instanceof CanceledException){
                    callback.onFailed(request,e);
                    return;
                }
            }
        }
        if(!isCanceled()){
            onResponse(response);
        }else{
            callback.onFailed(request,new CanceledException("URLTask has been canceled"));
        }
    }

    @Override
    public Response execute(URLite lite) throws Exception {
        Request real = request;
        String urlStr = real.getUrl();
        URL url = new URL(urlStr);
        HttpURLConnection connection;
        if (lite.settings.getProxy() != null) {
            connection = (HttpURLConnection) url.openConnection(lite.settings.getProxy());
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        assertCanceled();
        connection.setReadTimeout(lite.settings.getReadTimeout());
        connection.setConnectTimeout(lite.settings.getConnectTimeout());
        connection.setInstanceFollowRedirects(lite.settings.isFollowRedirects());
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
            httpsURLConnection.setSSLSocketFactory(lite.settings.getSslSocketFactory());
            httpsURLConnection.setHostnameVerifier(lite.settings.getHostnameVerifier());
        }
        Map<String, List<String>> headers = new HashMap<>();
        lite.processCookie(urlStr,headers);
        if(lite.isCacheAble(this))
            lite.addCacheHeaders(request,headers);
        if (real.getHeaders()!=null&&!real.getHeaders().isEmpty()) {
            boolean first;
            for (String name : real.getHeaders().keySet()) {
                first = true;
                for (String value : real.getHeaders().get(name)) {
                    if (first) {
                        connection.setRequestProperty(name, value);
                        first = false;
                    } else {
                        connection.addRequestProperty(name, value);
                    }
                }
            }
        }
        connection.setRequestMethod(real.getMethod().name());

        connection.setDoInput(true);
        RequestBody requestBody = lite.realBody(real.getRequestBody());
        if(real.getMethod().permitsRequestBody&&requestBody!=null){
            connection.setRequestProperty("Content-Type", requestBody.contentType());
            long contentLength = requestBody.contentLength();
            if (contentLength < 0) {
                connection.setChunkedStreamingMode(256 * 1024);
            } else {
                if (contentLength < Integer.MAX_VALUE) {
                    connection.setFixedLengthStreamingMode((int) contentLength);
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    connection.setFixedLengthStreamingMode(contentLength);
                } else {
                    connection.setChunkedStreamingMode(256 * 1024);
                }
            }
            connection.setRequestProperty("Content-Length", String.valueOf(contentLength));
            connection.setDoOutput(true);
            requestBody.writeTo(connection.getOutputStream());
        }

        Response response = URLite.createResponse(connection, real);
        lite.saveCookie(urlStr,response.headers());
        isExecuted = true;
        if(!lite.isCacheAble(this)){
            return response;
        }else{
            return lite.createCacheResponse(response);
        }
    }

    private void assertCanceled() throws Exception{
        if(isCanceled()){
            throw new CanceledException("URLTask has been canceled");
        }
    }

    public Object tag(){
        return request.getTag();
    }

    @Override
    public Request request() {
        return request;
    }

    public void cancel(){
        isCanceled = true;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isExecuted() {
        return isExecuted;
    }

    @Override
    public void setHandle(Handle handle) {
        throw new IllegalOperationException("not support method");
    }

    void onResponse(Response response){
        if(callback!=null){
            callback.onSuccess(request,response.headers(),response);
        }else {
            LogUtil.e("callback is null");
        }
    }

    @Override
    public int compareTo(Task another) {
        return hashCode()-another.hashCode();
    }
}
