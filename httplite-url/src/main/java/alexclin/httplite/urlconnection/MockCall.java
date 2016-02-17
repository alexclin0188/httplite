package alexclin.httplite.urlconnection;

import java.io.File;

import alexclin.httplite.Call;
import alexclin.httplite.CallFactory;
import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.Handle;
import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;

/**
 * MockCall
 *
 * @author alexclin
 * @date 16/1/29 21:38
 */
public class MockCall implements Call, Task ,Handle{
    Request request;
    private Factory factory;
    private Runnable runnable;
    private boolean isCanceled;
    private volatile boolean isEecuted;

    private MockCall(Request request, Factory mock) {
        this.request = request;
        this.factory = mock;
    }

    @Override
    public <T> Handle execute(Callback<T> callback) {
        mock(request, callback);
        return this;
    }

    @Override
    public Response executeSync() throws Exception {
        return execute();
    }

    @Override
    public <T> T executeSync(Clazz<T> clazz) throws Exception {
        return mockSync(request, clazz);
    }

    @Override
    public DownloadHandle download(Callback<File> callback) {
        return mock(request, callback).handle();
    }

    public <T> T mockSync(Request request, Clazz<T> clazz) throws Exception {
        MockResponse<T> response = new MockResponse<T>(clazz, this);
        factory.mockHandler.mock(request, response);
        return response.responseObject();
    }

    public <T> MockResponse<T> mock(final Request request, final Callback<T> callback) {
        final MockResponse<T> mockResponse = new MockResponse<T>(Clazz.of(callback), this);
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    factory.mockHandler.mock(request, mockResponse);
                    HttpLite.runOnMainThread(new Runnable() {
                        @Override
                        public void run() {
                            mockResponse.performCallback(callback);
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
            }
        };
        dispatch();
        return mockResponse;
    }

    void dispatch() {
        factory.liteClient.getDispatcher().dispatch(this);
    }

    void reset() {
        isCanceled = false;
    }

    @Override
    public void executeAsync() {
        if (runnable != null) runnable.run();
        isEecuted = true;
    }

    @Override
    public Response execute() throws Exception {
        MockResponse<Response> response = new MockResponse<>(new Clazz<Response>() {
        }, this);
        factory.mockHandler.mock(request, response);
        isEecuted = true;
        return response.response();
    }

    @Override
    public Object tag() {
        return request.getTag();
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

    public static class Factory implements CallFactory {

        private MockHandler mockHandler;
        private URLite liteClient;

        public Factory(MockHandler mockHandler, URLite lite) {
            this.mockHandler = mockHandler;
            this.liteClient = lite;
        }

        @Override
        public Call newCall(Request request) {
            return new MockCall(request, this);
        }
    }
}
