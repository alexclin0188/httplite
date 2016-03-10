package alexclin.httplite.listener;

import alexclin.httplite.Request;
import alexclin.httplite.mock.Mock;

/**
 * MockHandler
 *
 * @author alexclin
 * @date 16/1/29 20:33
 */
public interface MockHandler {
    <T> void mock(Request request,Mock<T> mock) throws Exception;
}
