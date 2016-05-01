package alexclin.httplite.mock;

import alexclin.httplite.Call;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.HttpCall;
import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MockHandler;

/**
 * MockCall
 *
 * @author alexclin 16/1/29 21:38
 */
public class MockCall extends HttpCall{
    MockFactory factory;

    public MockCall(Request request, MockFactory mock) {
        super(request);
        this.factory = mock;
    }

    @Override
    public <T> void async(boolean callOnMain,Callback<T> callback) {
        mock(callback,callOnMain);
    }

    @Override
    public <T> T sync(Clazz<T> clazz) throws Exception {
        return mockSync(clazz);
    }

    @SuppressWarnings("unchecked")
    private  <T> T mockSync(Clazz<T> clazz) throws Exception {
        MockTask<T> task = new MockTask<>(this,clazz,true);
        return (T)factory.dispatcher().execute(task);
    }

    @SuppressWarnings("unchecked")
    private  <T> MockTask<T> mock(final Callback<T> callback,boolean callOnMain) {
        MockTask<T> mockTask = new MockTask<>(this,callback,callOnMain);
        factory.dispatcher().dispatch(mockTask);
        return mockTask;
    }

    MediaType parse(String type) {
        return mediaType(type);
    }

    <T> T parseResultFrom(Response response,Clazz<T> clazz) throws Exception{
        return parseResult(response, createResultCallback(clazz,true));
    }

    public static class MockFactory extends HttpCall.Factory {

        MockHandler mockHandler;
        private Dispatcher taskDispatcher;

        public MockFactory(MockHandler mockHandler) {
            this.mockHandler = mockHandler;
        }

        @Override
        public Call newCall(Request request) {
            if(mockHandler.needMock(request)) {
                return new MockCall(request, this);
            }else{
                return super.newCall(request);
            }
        }

        public void cancel(Object tag){
            if(taskDispatcher!=null)
                dispatcher().cancel(tag);
        }

        private synchronized Dispatcher dispatcher(){
            if(taskDispatcher == null){
                taskDispatcher = new TaskDispatcher();
            }
            return taskDispatcher;
        }

        public void cancelAll(){
            if(taskDispatcher!=null)
                dispatcher().cancelAll();
        }

        public void shutDown(){
            if(taskDispatcher!=null)
                dispatcher().shutdown();
        }

        public <T> void callMock(Request request, Mock<T> mock) throws Exception{
            mockHandler.mock(request,mock);
        }
    }
}
