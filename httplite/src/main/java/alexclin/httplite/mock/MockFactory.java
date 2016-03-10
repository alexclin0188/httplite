package alexclin.httplite.mock;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.Call;
import alexclin.httplite.CallFactory;
import alexclin.httplite.Clazz;
import alexclin.httplite.Request;
import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.util.Util;

/**
 * MockFactory
 *
 * @author alexclin  16/3/10 20:36
 */
public class MockFactory implements CallFactory {

    MockHandler mockHandler;
    private ExecutorService mockExecutor;

    public MockFactory(MockHandler mockHandler) {
        this.mockHandler = mockHandler;
        this.mockExecutor = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(), Util.threadFactory("Mock Dispatcher", false));
    }

    @Override
    public Call newCall(Request request) {
        return new MockCall(request, this);
    }

    void dispatch(MockCall call){
        mockExecutor.submit(call.runnable);
    }

    <T> T dispatchSync(MockCall call,Clazz<T> clazz) throws Exception{
        return call.onSync(clazz);
    }

    public void cancel(Object tag){

    }
}