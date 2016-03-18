package alexclin.httplite.internal;

import alexclin.httplite.Clazz;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;

/**
 * MockTask
 *
 * @author alexclin  16/3/12 13:15
 */
public class MockTask<T> implements Dispatcher.Task<T>,Handle {
    private MockCall call;
    private boolean isCanceled;
    private volatile boolean isExecuted;
    private Clazz<T> clazz;
    private Callback<T> callback;
    private Mock<T> mock;
    private boolean callOnMain;

    public MockTask(MockCall call, Clazz<T> clazz,boolean callOnMain) {
        this.call = call;
        this.clazz = clazz;
        this.mock = new Mock<>(this);
        this.callOnMain = callOnMain;
    }

    public MockTask(MockCall call, Callback<T> callback,boolean callOnMain) {
        this(call, Clazz.of(callback),callOnMain);
        this.callback = callback;
    }

    Clazz<T> clazz(){
        return clazz;
    }

    T parseResult(Response response) throws Exception{
        return call.parseResultFrom(response, clazz);
    }

    MediaType parse(String type) {
        return call.parse(type);
    }

    @Override
    public Request request() {
        return call.request();
    }

    @Override
    public void executeAsync() {
        try {
            call.factory.callMock(request(), mock);
            mock.processMock();
            if(callOnMain)
            HttpLite.postOnMain(new Runnable() {
                @Override
                public void run() {
                    mock.performCallback(callback);
                }
            });
            else
                mock.performCallback(callback);
        } catch (final Exception e) {
            if(callOnMain){
                HttpLite.postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        callback.onFailed(request(), e);
                    }
                });
            }else{
                callback.onFailed(request(), e);
            }
        }
        isExecuted = true;
    }

    @Override
    public T execute() throws Exception {
        call.factory.callMock(request(), mock);
        mock.processMock();
        return mock.result();
    }

    @Override
    public Object tag() {
        return request().getTag();
    }

    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public boolean isExecuted() {
        return isExecuted;
    }

    @Override
    public void cancel() {
        isCanceled = true;
    }

    @Override
    public boolean resume() {
        if(isExecuted){
            isCanceled = false;
            executeAsync();
            return true;
        }
        return false;
    }
}
