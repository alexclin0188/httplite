package alexclin.httplite.listener;

import java.lang.reflect.Type;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;

/**
 * RequestListener
 *
 * @author alexclin 16/1/18 21:37
 */
public interface RequestListener {
    void onRequestStart(Request request,Type resultType);
    void onRequestEnd(Request request,Type resultType,Response response);
}
