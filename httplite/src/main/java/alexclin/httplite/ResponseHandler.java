//package alexclin.httplite;
//
//
//import java.lang.reflect.Type;
//import java.util.List;
//import java.util.Map;
//
//import alexclin.httplite.exception.CanceledException;
//import alexclin.httplite.impl.ObjectParser;
//import alexclin.httplite.listener.Callback;
//import alexclin.httplite.listener.Response;
//import alexclin.httplite.listener.ResponseListener;
//import alexclin.httplite.util.Util;
//
///**
// * ResponseHandler
// *
// * @author alexclin  16/1/1 19:05
// */
//public class ResponseHandler<T> {
//    protected volatile boolean isCanceled;
//    protected Callback<T> callback;
//    protected HttpCall call;
//    private boolean callOnMain;
//    private Type type;
//
//    private ObjectParser.Cancelable cancelable = new ObjectParser.Cancelable() {
//        @Override
//        public boolean isCanceled() {
//            return isCanceled;
//        }
//    };
//
//    protected ResponseHandler(Callback<T> callback, HttpCall call,Type type, boolean callOnMain) {
//        this.callback = callback;
//        this.call = call;
//        this.callOnMain = callOnMain;
//        this.type = type;
//    }
//
//    public final void onResponse(Response response) {
//        if (isCanceled) {
//            callCancelAndFailed();
//            return;
//        }
//        ResponseListener filter = getLite().getResponseFilter();
//        if (filter != null) filter.onResponse(getLite(), call.request.build(), response);
//        response = call.request.build().handleResponse(response);
//        try {
//            T result = parseResponse(response);
//            postSuccess(result, response.headers());
//        }catch (Exception e) {
//            postFailed(e);
//        }
//    }
//
//    protected Type resultType() {
//        return type;
//    }
//
//    public final void onFailed(Exception e) {
//        postFailed(e);
//    }
//
//    public final void onCancel() {
//        isCanceled = true;
//    }
//
//    protected final void postFailed(final Exception e) {
//        if (callOnMain)
//            HttpLite.runOnMain(new Runnable() {
//                @Override
//                public void run() {
//                    callback.onFailed(call.request.build(), e);
//                }
//            });
//        else
//            callback.onFailed(call.request.build(), e);
//    }
//
//    protected final void postSuccess(final T result, final Map<String, List<String>> headers) {
//        if (callOnMain&&!(result instanceof Response))
//            HttpLite.runOnMain(new Runnable() {
//                @Override
//                public void run() {
//                    callback.onSuccess(call.request.build(),headers,result);
//                }
//            });
//        else {
//            callback.onSuccess(call.request.build(), headers, result);
//            if(result instanceof Response){
//                Util.closeQuietly(((Response) result).body());
//            }
//        }
//    }
//
//    protected final HttpLite getLite() {
//        return call.request.build().lite();
//    }
//
//    T parseResponse(Response response) throws Exception{
//        return getLite().getObjectParser().parseObject(response,resultType(),cancelable);
//    }
//
//    public void callCancelAndFailed(CanceledException e) {
//        onCancel();
//        if (e == null)
//            e = new CanceledException("Request is canceled");
//        postFailed(e);
//    }
//
//    public void callCancelAndFailed() {
//        callCancelAndFailed(null);
//    }
//}
