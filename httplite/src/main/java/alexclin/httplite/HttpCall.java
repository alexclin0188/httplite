package alexclin.httplite;

import java.io.File;
import java.lang.reflect.Type;
import java.util.concurrent.Executor;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Util;

/**
 * HttpCall
 *
 * @author alexclin at 16/1/26 22:12
 */
public class HttpCall extends Call{
    protected Request request;

    protected HttpCall(Request request) {
        this.request = request;
    }

    @Override @SuppressWarnings("unchecked")
    public <T> Handle async(boolean callOnMain, Callback<T> callback) {
        Type type = Util.type(Callback.class, callback);
        if(type==File.class){
            final DownloadCallback rcb = createDownloadCallback((Callback<File>) callback,callOnMain);
            executeSelf(rcb);
            return rcb;
        }else{
            ResultCallback rcb = createHttpCallback(callback, type,callOnMain);
            return executeSelf(rcb);
        }
    }

    @Override
    public <T> T sync(Clazz<T> clazz) throws Exception{
        ResultCallback<T> callback = createResultCallback(clazz,true);
        Response response = executeSyncInner(callback);
        return parseResult(response, callback);
    }

    /***************************************/

    @SuppressWarnings("unchecked")
    protected  <T> ResultCallback<T> createResultCallback(Clazz<T> clazz,boolean callOnMain) {
        Type type = clazz.type();
        ResultCallback<T> callback;
        if(type==File.class) {
            callback = (ResultCallback<T>)(this.<File>createDownloadCallback(null,callOnMain));
        }else{
            callback = this.<T>createHttpCallback(null, type,callOnMain);
        }
        return callback;
    }

    protected  <T> ResultCallback createHttpCallback(Callback<T> callback, Type type,boolean callOnMain) {
        HttpLite lite = request.lite;
        ResultCallback<T> rcb = new HttpCallback<>(callback,this,type,callOnMain);
        if(lite.getRequestFilter()!=null) lite.getRequestFilter().onRequest(lite,request, type);
        return rcb;
    }

    protected <T> T parseResult(Response response, ResultCallback<T> callback) throws Exception{
        return callback.parseResponse(response);
    }

    protected MediaType mediaType(String mediaType){
        return request.lite.parse(mediaType);
    }

    protected DownloadCallback createDownloadCallback(Callback<File> callback,boolean callOnMain) {
        DownloadCallback.DownloadParams params = request.getDownloadParams();
        if(params==null){
            throw new IllegalArgumentException("to execute Callback<File>, you must call intoFile() on Request before execute");
        }
        return new DownloadCallback(callback,this,params,callOnMain);
    }

    private Response executeSyncInner(ResultCallback callback) throws Exception{
        Runnable preWork = null;
        if(callback instanceof DownloadCallback){
            preWork = (DownloadCallback)callback;
        }
        HttpLite lite = request.lite;
        if(lite.getRequestFilter()!=null) lite.getRequestFilter().onRequest(lite,request,callback.resultType());
        if(preWork!=null) preWork.run();
        Response response = lite.getClient().executeSync(request);
        if(lite.getResponseFilter()!=null) lite.getResponseFilter().onResponse(lite,request, response);
        return response;
    }

    <T> Handle executeSelf(final ResultCallback<T> callback){
        HttpLite lite = request.lite;
        boolean isDownload = callback instanceof DownloadCallback;
        final Runnable preWork = isDownload?(DownloadCallback)callback:null;
        final Executor executor = lite.getCustomDownloadExecutor();
        if(isDownload&&executor!=null){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        callback.onResponse(executeSyncInner(callback));
                    } catch (Exception e) {
                        callback.onFailed(e);
                    }
                }
            });
            return (DownloadCallback)callback;
        }else{
            Handle handle = lite.getClient().execute(request,callback,preWork);
            return isDownload?((DownloadCallback)callback).wrap(handle):handle;
        }
    }

    public static class Factory implements CallFactory{

        @Override
        public Call newCall(Request request) {
            return new HttpCall(request);
        }
    }
}
