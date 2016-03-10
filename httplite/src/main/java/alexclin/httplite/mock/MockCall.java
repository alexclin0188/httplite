package alexclin.httplite.mock;

import java.io.File;

import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpCall;
import alexclin.httplite.HttpLite;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;

/**
 * MockCall
 *
 * @author alexclin
 * @date 16/1/29 21:38
 */
public class MockCall extends HttpCall implements Handle{
    private MockFactory factory;
    private boolean isCanceled;
    private volatile boolean isEecuted;
    Runnable runnable;

    MockCall(Request request, MockFactory mock) {
        super(request);
        this.factory = mock;
    }

    @Override
    public <T> Handle execute(Callback<T> callback) {
        mock(callback);
        return this;
    }

    @Override
    public Response executeSync() throws Exception {
        return mockSync(new Clazz<Response>() {});
    }

    @Override
    public <T> T executeSync(Clazz<T> clazz) throws Exception {
        return mockSync(clazz);
    }

    @Override
    public DownloadHandle download(Callback<File> callback) {
        return mock(callback).handle();
    }

    @SuppressWarnings("unchecked")
    public <T> T mockSync(Clazz<T> clazz) throws Exception {
        return factory.dispatchSync(this, clazz);
    }

    <T> T onSync(Clazz<T> clazz) throws Exception{
        Mock<T> response = new Mock<T>(clazz, this);
        factory.mockHandler.mock(request, response);
        response.processMock();
        return response.responseObject();
    }

    public <T> Mock<T> mock(final Callback<T> callback) {
        final Mock<T> mock = new Mock<T>(Clazz.of(callback), this);
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    factory.mockHandler.mock(request, mock);
                    mock.processMock();
                    HttpLite.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            mock.performCallback(callback);
                        }
                    });
                } catch (final Exception e) {
                    HttpLite.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            callback.onFailed(request, e);
                        }
                    });
                }
                isEecuted = true;
            }
        };
        dispatch();
        return mock;
    }

    public void dispatch(){
        factory.dispatch(this);
    }

    void reset() {
        isCanceled = false;
    }

    @Override
    public boolean isCanceled() {
        return isCanceled;
    }

    @Override
    public boolean isExecuted() {
        return isEecuted;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public void cancel() {
        isCanceled = true;
    }

    MediaType prase(String type) {
        return mediaType(type);
    }

    <T> T parseResultFrom(Response response,Clazz<T> clazz) throws Exception{
        return parseResult(response, createResultCallback(clazz));
    }
}
