package alexclin.httplite.listener;

import alexclin.httplite.Request;
import alexclin.httplite.internal.Mock;

/**
 * MockHandler
 *
 * @author alexclin 16/1/29 20:33
 */
public interface MockHandler {
    <T> void mock(Request request,Mock<T> mock) throws Exception;

    boolean needMock(Request request);
}
