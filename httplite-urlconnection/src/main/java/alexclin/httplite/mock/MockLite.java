package alexclin.httplite.mock;

import alexclin.httplite.Call;
import alexclin.httplite.Clazz;
import alexclin.httplite.HttpLite;
import alexclin.httplite.HttpLiteBuilder;
import alexclin.httplite.LiteClient;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.urlconnection.URLConnectionLite;

/**
 * MockLite
 *
 * @author alexclin
 * @date 16/1/29 20:45
 */
public class MockLite extends HttpLiteBuilder{
    private MockHandler mockHandler;

    public static HttpLiteBuilder mock(MockHandler mockHandler){
        return new MockLite(mockHandler);
    }

    private MockLite(MockHandler mockHandler) {
        this.mockHandler = mockHandler;
    }

    public Response mockSync(Request request) throws Exception{
        MockResponse response = new MockResponse(null,request);
        mockHandler.mock(request,response);
        return response.response();
    }

    public <T> T mockSync(Request request,Clazz<T> clazz) throws Exception{
        MockResponse<T> response = new MockResponse<T>(clazz,request);
        mockHandler.mock(request,response);
        return response.responseObject();
    }

    public <T> MockResponse<T> mock(final Request request,final Callback<T> callback){
        final MockResponse<T> response = new MockResponse<T>(Clazz.of(callback),request);
        try {
            mockHandler.mock(request, response);
            HttpLite.runOnMainThread(new Runnable() {
                @Override
                public void run() {
                    response.performCallback(callback);
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
        return response;
    }

    @Override
    protected LiteClient initLiteClient() {
        return new URLConnectionLite();
    }

    @Override
    public Call newCall(Request request) {
        return new MockCall(request,this);
    }
}
