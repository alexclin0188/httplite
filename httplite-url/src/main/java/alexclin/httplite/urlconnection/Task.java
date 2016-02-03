package alexclin.httplite.urlconnection;

import alexclin.httplite.Response;

/**
 * Task
 *
 * @author alexclin
 * @date 16/2/3 18:49
 */
public interface Task {
    void executeAsync();
    Response execute() throws Exception;
    Object tag();
    boolean isCanceled();
    boolean isExecuted();
    void cancel();
}
