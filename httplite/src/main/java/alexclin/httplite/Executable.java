package alexclin.httplite;

import java.io.IOException;

/**
 * Executable
 *
 * @author alexclin  16/4/23 10:32
 */
public interface Executable {
    Response execute() throws Exception;

    Handle enqueue(ResponseHandler responseHandler);

    void cancel();

    boolean isExecuted();

    boolean isCanceled();
}
