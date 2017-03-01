package alexclin.httplite.mock;

import java.lang.reflect.Type;

import alexclin.httplite.Request;
import alexclin.httplite.Result;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Clazz;

/**
 * @author xiehonglin429 on 2017/3/1.
 */

public class MockLite {

    public boolean needMock(Request request){

        return false;
    }

    public <T> Result<T> mockExecute(Request request, Type type) {
        return null;
    }

    public <T> void mockEnqueue(Request request, Callback<T> callback) {

    }

    public void cancel(Object tag) {
        //TODO
    }

    public void cancelAll() {
        //TODO
    }

    public void shutDown() {
        //TODO
    }
}
