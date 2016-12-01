package alexclin.httplite.okhttp3;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.Executable;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.ResponseHandler;
import alexclin.httplite.exception.CanceledException;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.OkHttpClient;

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
        okhttp3.Request req = createRequestBuilder(request).build();
        realCall = mClient.newCall(req);
        return new OkResponse(realCall.execute(),request.build());
    }

    @Override
    public void enqueue(final ResponseHandler responseHandler) {
        setRealCall(executeInternal(request, responseHandler));
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
        if (realCall == null)
            return false;
        else
            return realCall.isExecuted();
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

    private Call executeInternal(final alexclin.httplite.Request.Builder request, final ResponseHandler callback){
        okhttp3.Request.Builder rb = createRequestBuilder(request);
        Call call = mClient.newCall(rb.build());
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if("Canceled".equals(e.getMessage())){
                    callback.onFailed(new CanceledException(e));
                }else{
                    callback.onFailed(e);
                }
            }

            @Override
            public void onResponse(Call call, okhttp3.Response response) throws IOException {
                callback.onResponse(new OkResponse(response, request.build()));
            }
        });
        return call;
    }

    private okhttp3.Request.Builder createRequestBuilder(alexclin.httplite.Request.Builder request) {
        Request real = request.build();
        okhttp3.Request.Builder rb = new okhttp3.Request.Builder().url(real.getUrl()).tag(real.getTag());
        Headers okheader = createHeader(real.getHeaders());
        if(okheader!=null){
            rb.headers(okheader);
        }
        switch (real.getMethod()){
            case GET:
                rb = rb.get();
                break;
            case POST:
                rb = rb.post(OkRequestBody.wrapperLite(real.getBody()));
                break;
            case PUT:
                rb = rb.put(OkRequestBody.wrapperLite(real.getBody()));
                break;
            case PATCH:
                rb = rb.patch(OkRequestBody.wrapperLite(real.getBody()));
                break;
            case HEAD:
                rb = rb.head();
                break;
            case DELETE:
                if(request.build().getBody()==null){
                    rb = rb.delete();
                }else{
                    rb = rb.delete(OkRequestBody.wrapperLite(real.getBody()));
                }
                break;
        }
        if(real.getCacheExpiredTime()>0){
            rb.cacheControl(new CacheControl.Builder().maxAge(real.getCacheExpiredTime(), TimeUnit.SECONDS).build());
        }else if(real.getCacheExpiredTime()== alexclin.httplite.Request.FORCE_CACHE){
            rb.cacheControl(CacheControl.FORCE_CACHE);
        }else if(real.getCacheExpiredTime()== alexclin.httplite.Request.NO_CACHE){
            rb.cacheControl(CacheControl.FORCE_NETWORK);
        }
        return rb;
    }
}
