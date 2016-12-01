package alexclin.httplite.url;

import android.os.Build;

import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import alexclin.httplite.Executable;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.ResponseHandler;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.Dispatcher;

/**
 * URLTask
 *
 * @author alexclin 16/1/2 19:39
 */
public class URLTask implements Dispatcher.Task<Response>,Comparable<Dispatcher.Task<Response>>,Executable{

    private URLite lite;
    private Request.Builder request;
    private int retryCount;

    private ResponseHandler callback;

    private volatile boolean isExecuted;
    private volatile boolean isCanceled;

    public URLTask(URLite lite, Request.Builder request) {
        this.lite = lite;
        this.request = request;
    }

    public void enqueueTask() {
        Response response = null;
        int maxRetry = lite.settings.getMaxRetryCount();
        while (retryCount<= maxRetry&& !isCanceled()){
            try {
                retryCount++;
                response = execute();
                if(response!=null){
                    break;
                }
            }catch (Exception e) {
                e.printStackTrace();
                if(retryCount>maxRetry || e instanceof CanceledException){
                    callback.onFailed(e);
                    return;
                }
            }
        }
        if(!isCanceled()){
            onResponse(response);
        }else{
            callback.onFailed(new CanceledException("URLTask has been canceled"));
        }
    }

    public Response executeTask() throws Exception {
        if(lite.isCacheAble(this)) lite.addCacheHeaders(request);
        Request real = request.build();
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
        lite.processCookie(urlStr,real.getHeaders());
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
        if(real.getMethod().permitsRequestBody&&real.getBody()!=null){
            connection.setRequestProperty("Content-Type", real.getBody().contentType().toString());
            long contentLength = real.getBody().contentLength();
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
            real.getBody().writeTo(connection.getOutputStream());
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

    @Override
    public Response execute() throws Exception {
        return lite.dispatchTaskSync(this);
    }

    @Override
    public void enqueue(ResponseHandler responseHandler) {
        this.callback = responseHandler;
        lite.dispatchTask(this);
    }

    private void assertCanceled() throws Exception{
        if(isCanceled()){
            throw new CanceledException("URLTask has been canceled");
        }
    }

    public Object tag(){
        return request.build().getTag();
    }

    @Override
    public Request request() {
        return request.build();
    }

    public void cancel(){
        isCanceled = true;
        callback.onCancel();
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isExecuted() {
        return isExecuted;
    }

    public void onResponse(Response response){
        callback.onResponse(response);
    }

    @Override
    public int compareTo(Dispatcher.Task<Response> another) {
        return hashCode()-another.hashCode();
    }
}
