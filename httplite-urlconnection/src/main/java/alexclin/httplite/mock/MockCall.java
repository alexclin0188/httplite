package alexclin.httplite.mock;

import java.io.File;

import alexclin.httplite.Call;
import alexclin.httplite.Clazz;
import alexclin.httplite.DownloadHandle;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;

/**
 * MockCall
 *
 * @author alexclin
 * @date 16/1/29 21:38
 */
public class MockCall implements Call{
    private Request request;
    private MockLite mock;

    MockCall(Request request, MockLite mock) {
        this.request = request;
        this.mock = mock;
    }

    @Override
    public <T> void execute(Callback<T> callback) {
        mock.mock(request,callback);
    }

    @Override
    public Response executeSync() throws Exception {
        return mock.mockSync(request);
    }

    @Override
    public <T> T executeSync(Clazz<T> clazz) throws Exception {
        return mock.mockSync(request,clazz);
    }

    @Override
    public DownloadHandle download(Callback<File> callback) {
        return mock.mock(request,callback).handle();
    }
}
