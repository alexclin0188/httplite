package alexclin.httplite;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import alexclin.httplite.impl.ObjectParser;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.MockHandler;
import alexclin.httplite.listener.Response;

/**
 * @author xiehonglin429 on 2017/3/1.
 */

class MockLite {
    private MockHandler handler;
    private ObjectParser parser;
    private ExecutorService executor;
    private CopyOnWriteArraySet<Request> tasks;
    private HttpLite lite;

    MockLite(MockHandler handler, ObjectParser parser, ExecutorService executor, HttpLite lite) {
        this.handler = handler;
        this.parser = parser;
        this.executor = executor;
        this.tasks = new CopyOnWriteArraySet<>();
        this.lite = lite;
    }

    boolean needMock(Request request){
        return handler.needMock(request);
    }

    <T> Result<T> execute(Request request, Type type) {
        tasks.add(request);
        Mock<T> mock = new Mock<>(request,type,this);
        Result<T> result = mock.execute();
        tasks.remove(request);
        return result;
    }

    public <T> void enqueue(Request request, Callback<T> callback) {
        tasks.add(request);
        getExecutor().submit(new Mock<T>(request,callback,this).getTask());
    }

    public void cancel(Object tag) {
        List<Request> cancels = new ArrayList<>();
        for(Request request:tasks){
            if (equals(request.getTag(),tag)){
                request.handle().cancel();
                cancels.add(request);
            }
        }
        if(!cancels.isEmpty()) tasks.removeAll(cancels);
    }

    private boolean equals(Object one, Object two){
        return one!=null&&one.equals(two);
    }

    void cancelAll() {
        List<Request> requests = new ArrayList<>(tasks);
        tasks.removeAll(requests);
        for(Request request:requests){
            request.handle().cancel();
        }
    }

    void shutDown() {
        if(this.executor!=null){
            this.executor.shutdownNow();
            this.executor = null;
        }
    }

    <T> T parseResponse(Response response,Type type) throws Exception{
        return parser.parseObject(response,type);
    }

    private ExecutorService getExecutor() {
        if(this.executor==null){
            synchronized (this){
                if(this.executor==null){
                    this.executor = Executors.newCachedThreadPool();
                }
            }
        }
        return executor;
    }

    <T> void mock(Request request,Mock<T> mock) throws Exception{
        handler.mock(request,mock);
    }

    public MediaType mediaType(String mediaType){
        return lite.mediaType(mediaType);
    }
}
