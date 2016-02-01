package alexclin.httplite.urlconnection;

import android.os.Build;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Method;
import alexclin.httplite.Request;
import alexclin.httplite.RequestBody;
import alexclin.httplite.Response;
import alexclin.httplite.ResultCallback;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.util.LogUtil;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/2 19:39
 */
public class URLTask {

    private URLConnectionLite lite;
    private String url;
    private Method method;
    private Map<String, List<String>> headers;
    private RequestBody requestBody;
    private Object tag;
    private Request request;
    private int retryCount;

    private ResultCallback callback;
    private Runnable preWork;

    private volatile boolean isExecuted;
    private volatile boolean isCanceled;

    public URLTask(URLConnectionLite lite,String url, Method method, Map<String, List<String>> headers, RequestBody body,
                   Object tag, Request request,ResultCallback callback,Runnable runnable) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.requestBody = body;
        this.tag = tag;
        this.request = request;
        this.lite = lite;
        this.callback = callback;
        this.preWork = runnable;
    }

    public void executeAsync() {
        if(preWork!=null){
            preWork.run();
        }
        Response response = null;
        while (retryCount<=lite.maxRetry && !isCanceled()){
            try {
                if(retryCount>0){
                    callback.onRetry(retryCount,lite.maxRetry);
                }
                retryCount++;
                response = execute();
            }catch (Exception e) {
                if(retryCount>lite.maxRetry || e instanceof CanceledException){
                    callback.onFailed(e);
                    return;
                }
            }
            if(response!=null){
                break;
            }
        }
        if(!isCanceled()){
            callback.onResponse(response);
        }else{
            callback.onFailed(new CanceledException("URLTask has been canceled"));
        }
    }

    public Response execute() throws Exception {
        URL url = new URL(this.url);
        HttpURLConnection connection;
        if (lite.proxy != null) {
            connection = (HttpURLConnection) url.openConnection(lite.proxy);
        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        assertCenceled();
        connection.setReadTimeout(lite.readTimeout);
        connection.setConnectTimeout(lite.connectTimeout);
        connection.setInstanceFollowRedirects(lite.followRedirects);

        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) connection;
            httpsURLConnection.setSSLSocketFactory(lite.sslSocketFactory);
            httpsURLConnection.setHostnameVerifier(lite.hostnameVerifier);
        }

        if (headers!=null) {
            boolean first;
            for (String name : headers.keySet()) {
                first = true;
                for (String value : headers.get(name)) {
                    if (first) {
                        connection.setRequestProperty(name, value);
                        first = false;
                    } else {
                        connection.addRequestProperty(name, value);
                    }
                }
            }
        }
        connection.setRequestMethod(method.name());
        if(Request.permitsRequestBody(method)&&requestBody!=null){
            connection.setRequestProperty("Content-Type", requestBody.contentType().toString());
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
        Response response = new URLResponse(connection,request);
        isExecuted = true;
        return response;
    }

    private void assertCenceled() throws Exception{
        if(isCanceled()){
            throw new CanceledException("URLTask has been canceled");
        }
    }

    public Object tag(){
        return tag;
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
}
