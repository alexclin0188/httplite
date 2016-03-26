package alexclin.httplite.sample;

import java.lang.reflect.Type;

import alexclin.httplite.Response;

/**
 * ResponseHandler
 *
 * @author alexclin  16/3/23 22:47
 */
public interface ResponseHandler<T> {
    void handleResponse(Response response);
}

interface ResultParser{
    <T> T parseResult(Response response) throws Exception;
}

interface Call{
    <T> T execute();

}

interface ResultParserFactory{
    ResultParser create(Type type);
}
