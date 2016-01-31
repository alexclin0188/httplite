package alexclin.httplite.listener;

import java.io.IOException;
import java.lang.reflect.Type;

import alexclin.httplite.Response;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 21:15
 */
public interface ResponseParser {
    boolean isSupported(Type type);
    <T> T praseResponse(Response response, Type type) throws Exception;
}
