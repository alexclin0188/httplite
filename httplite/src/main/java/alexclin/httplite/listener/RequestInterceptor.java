package alexclin.httplite.listener;

import java.lang.reflect.Type;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;

/**
 * RequestInterceptor
 *
 * @author alexclin 16/1/18 21:37
 */
public interface RequestInterceptor {
    Request interceptRequest(Request request, Type resultType);
}
