//package alexclin.httplite;
//
//import java.io.File;
//import java.lang.reflect.Type;
//import java.util.concurrent.Executor;
//
//import alexclin.httplite.listener.Callback;
//import alexclin.httplite.listener.MediaType;
//import alexclin.httplite.listener.Response;
//import alexclin.httplite.util.Clazz;
//import alexclin.httplite.util.Util;
//
///**
// * HttpCall
// *
// * @author alexclin at 16/1/26 22:12
// */
//public class HttpCall extends Call{
//
//    protected HttpCall(Request.Builder request) {
//        super(request);
//    }
//
//    @Override @SuppressWarnings("unchecked")
//    public <T> Handle async(boolean callOnMain, Callback<T> callback) {
//        Type type = Util.type(Callback.class, callback);
//        ResponseHandler rcb;
//        if(type==File.class){
//            rcb = createDownloadCallback((Callback<File>) callback,callOnMain);
//        }else{
//            rcb = createHttpCallback(callback, type,callOnMain);
//        }
//        executeSelf(rcb);
//        return this;
//    }
//
//    @Override
//    public <T> T sync(Clazz<T> clazz) throws Exception{
//        ResponseHandler<T> callback = createResultCallback(clazz,true);
//        Response response = executeSyncInner(callback);
//        return parseResult(response, callback);
//    }
//
//    @Override
//    public <T> Result<T> syncResult(Clazz<T> clazz){
//        ResponseHandler<T> callback = createResultCallback(clazz, true);
//        Response response;
//        try {
//            response = executeSyncInner(callback);
//        } catch (Exception e) {
//            return new Result<T>(null,null,e);
//        }
//        T r = null;
//        try {
//            r = parseResult(response, callback);
//            return new Result<T>(r,response.headers());
//        } catch (Exception e) {
//            return new Result<T>(r,response.headers(),e);
//        }
//    }
//
//    @Override
//    public Request request() {
//        return request.build();
//    }
//
//    /***************************************/
//
//    @SuppressWarnings("unchecked")
//    protected  <T> ResponseHandler<T> createResultCallback(Clazz<T> clazz,boolean callOnMain) {
//        Type type = clazz.type();
//        ResponseHandler<T> callback;
//        if(type==File.class) {
//            callback = (ResponseHandler<T>)(this.<File>createDownloadCallback(null,callOnMain));
//        }else{
//            callback = this.<T>createHttpCallback(null, type,callOnMain);
//        }
//        return callback;
//    }
//
//    protected  <T> ResponseHandler createHttpCallback(Callback<T> callback, Type type,boolean callOnMain) {
//        HttpLite lite = request.build().lite();
//        ResponseHandler<T> rcb = new ResponseHandler<>(callback,this,type,callOnMain);
//        if(lite.getRequestFilter()!=null) lite.getRequestFilter().onRequestStart(request.build(), type);
//        return rcb;
//    }
//
//    protected <T> T parseResult(Response response, ResponseHandler<T> callback) throws Exception{
//        return callback.parseResponse(response);
//    }
//
//    protected MediaType mediaType(String mediaType){
//        return request.build().lite().parse(mediaType);
//    }
//
//    protected DownloadHandler createDownloadCallback(Callback<File> callback,boolean callOnMain) {
//        DownloadHandler.DownloadParams params = request.build().getDownloadParams();
//        if(params==null){
//            throw new IllegalArgumentException("to execute Callback<File>, you must call intoFile() on Request before execute");
//        }
//        return new DownloadHandler(callback,this,params,callOnMain);
//    }
//
//    private Response executeSyncInner(ResponseHandler callback) throws Exception{
//        HttpLite lite = request.build().lite();
//        if(callback instanceof DownloadHandler) ((DownloadHandler) callback).doResumeWork();
//        if(lite.getRequestFilter()!=null) lite.getRequestFilter().onRequestStart(request.build(),callback.resultType());
//        Executable executable = lite.getClient().executable(request);
//        if(callback instanceof DownloadHandler) executable = ((DownloadHandler) callback).wrap(executable);
//        setExecutable(executable);
//        Response response = executable().execute();
//        if(lite.getResponseFilter()!=null) lite.getResponseFilter().onResponse(lite,request.build(), response);
//        response = request.build().handleResponse(response);
//        return response;
//    }
//
//    <T> void executeSelf(final ResponseHandler<T> callback){
//        HttpLite lite = request.build().lite();
//        boolean isDownload = callback instanceof DownloadHandler;
//        if(callback instanceof DownloadHandler) ((DownloadHandler) callback).doResumeWork();
//        final Executor executor = lite.getCustomDownloadExecutor();
//        if(isDownload&&executor!=null){
//            executor.execute(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        callback.onResponse(executeSyncInner(callback));
//                    } catch (Exception e) {
//                        callback.onFailed(e);
//                    }
//                }
//            });
//        }else{
//            Executable executable = lite.getClient().executable(request);
//            if(isDownload){
//                executable = ((DownloadHandler)callback).wrap(executable);
//            }
//            setExecutable(executable);
//            executable().enqueue(callback);
//        }
//    }
//
//    public static class Factory implements CallFactory{
//
//        @Override
//        public Call newCall(Request.Builder request) {
//            return new HttpCall(request);
//        }
//    }
//}
