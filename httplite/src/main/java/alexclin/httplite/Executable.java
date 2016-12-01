package alexclin.httplite;

import alexclin.httplite.listener.Response;

/**
 * Executable
 *
 * @author alexclin  16/4/23 10:32
 */
public interface Executable {
    Response execute() throws Exception;

    void enqueue(ResponseHandler responseHandler);

    void cancel();

    boolean isExecuted();

    boolean isCanceled();
}
