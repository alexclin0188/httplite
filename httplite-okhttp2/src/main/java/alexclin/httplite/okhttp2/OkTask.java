package alexclin.httplite.okhttp2;

import com.squareup.okhttp.CacheControl;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.Executable;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.ResponseHandler;
import alexclin.httplite.exception.CanceledException;

/**
 * OkTask
 *
 * @author alexclin 16/2/17 19:46
 */
public class OkTask implements Executable {
    private Call realCall;
    private volatile boolean isCanceled = false;
    private Request.Builder request;
    private OkHttpClient mClient;

    public OkTask(final Request.Builder request, OkHttpClient client) {
        this.request = request;
        this.mClient = client;
    }

    static com.squareup.okhttp.Request.Builder createRequestBuilder(alexclin.httplite.Request.Builder builder) {
        Request request = builder.build();
        com.squareup.okhttp.Request.Builder rb = new com.squareup.okhttp.Request.Builder().url(request.getUrl()).tag(request.getTag());
        Headers headers = createHeader(request.getHeaders());
        if(headers!=null){
            rb.headers(headers);
        }
        switch (request.getMethod()){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(OkRequestBody.wrapperLite(request.getBody()));
                break;
            case PUT:
                rb = rb.put(OkRequestBody.wrapperLite(request.getBody()));
                break;
            case PATCH:
                rb = rb.patch(OkRequestBody.wrapperLite(request.getBody()));
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(request.getBody()==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(OkRequestBody.wrapperLite(request.getBody()));
                }
                break;
        }
        if(request.getCacheExpiredTime()>0){
            rb.cacheControl(new CacheControl.Builder().maxAge(request.getCacheExpiredTime(), TimeUnit.SECONDS).build());
        }else if(request.getCacheExpiredTime()== alexclin.httplite.Request.FORCE_CACHE){
            rb.cacheControl(CacheControl.FORCE_CACHE);
        }else if(request.getCacheExpiredTime()== alexclin.httplite.Request.NO_CACHE){
            rb.cacheControl(CacheControl.FORCE_NETWORK);
        }
        return rb;
    }

    static Headers createHeader(Map<String, List<String>> headers){
        if(headers!=null&&!headers.isEmpty()){
            Headers.Builder hb = new Headers.Builder();
            for(String key:headers.keySet()){
                List<String> values = headers.get(key);
                for(String value:values){
                    hb.add(key,value);
                }
            }
            return hb.build();
        }
        return null;
    }

    @Override
    public Response execute() throws IOException {
        com.squareup.okhttp.Request req = createRequestBuilder(request).build();
        realCall = mClient.newCall(req);
        return new OkResponse(realCall.execute(),request.build());
    }

    @Override
    public void enqueue(final ResponseHandler handler) {
        setRealCall(enqueueInternal(request, handler));
    }

    @Override
    public void cancel() {
        if (realCall == null) {
            isCanceled = true;
        } else {
            realCall.cancel();
        }
    }

    @Override
    public boolean isExecuted() {
        return  realCall != null && realCall.isExecuted();
    }

    @Override
    public boolean isCanceled() {
        if (realCall == null)
            return isCanceled;
        else
            return realCall.isCanceled();
    }

    void setRealCall(Call realCall) {
        this.realCall = realCall;
    }

    private Call enqueueInternal(final Request.Builder request, final ResponseHandler handler){
        com.squareup.okhttp.Request.Builder rb = createRequestBuilder(request);
        Call realCall = mClient.newCall(rb.build());
        realCall.enqueue(new Callback() {
            @Override
            public void onFailure(com.squareup.okhttp.Request request, IOException e) {
                if("Canceled".equals(e.getMessage())){
                    handler.onFailed(new CanceledException(e));
                }else{
                    handler.onFailed(e);
                }
            }

            @Override
            public void onResponse(com.squareup.okhttp.Response response) throws IOException {
                handler.onResponse(new OkResponse(response, request.build()));
            }
        });
        return realCall;
    }
}
