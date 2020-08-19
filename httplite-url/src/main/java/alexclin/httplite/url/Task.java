package alexclin.httplite.url;

import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;

/**
 * @author xiehonglin429 on 2017/3/19.
 */

interface Task {
    Request request();
    void executeCallback(URLite lite);
    Response execute(URLite lite) throws Exception;
    Object tag();
    boolean isCanceled();
    boolean isExecuted();
    void cancel();
}
