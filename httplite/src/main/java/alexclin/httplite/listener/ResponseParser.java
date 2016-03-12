package alexclin.httplite.listener;

import java.lang.reflect.Type;

import alexclin.httplite.Response;

/**
 * ResponseParser
 *
 * @author alexclin 16/1/1 21:15
 */
public interface ResponseParser {
    boolean isSupported(Type type);
    <T> T parseResponse(Response response, Type type) throws Exception;
}
