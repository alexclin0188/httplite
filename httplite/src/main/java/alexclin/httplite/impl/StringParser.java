package alexclin.httplite.impl;

import java.lang.reflect.Type;

import alexclin.httplite.Response;
import alexclin.httplite.listener.ResponseParser;

/**
 * StringParser
 *
 * @author alexclin  16/1/1 23:08
 */
public abstract class StringParser implements ResponseParser{

    @Override
    public final <T> T parseResponse(Response response, Type type) throws Exception{
        return parseResponse(ObjectParser.decodeToString(response), type);
    }

    protected abstract <T> T parseResponse(String content, Type type) throws Exception;
}
