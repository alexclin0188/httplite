package alexclin.httplite.listener;

import alexclin.httplite.Request;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 21:44
 */
public interface CancelListener {
    void onCancel(Request request);
}
