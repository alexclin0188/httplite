package alexclin.httplite.mock;

import alexclin.httplite.Request;

/**
 * Dispatcher
 *
 * @author alexclin 16/2/18 19:53
 */
public interface Dispatcher<T> {
    void dispatch(Task<T> task);

    T execute(Task<T> task) throws Exception;

    void cancel(Object tag);

    void cancelAll();

    void shutdown();

    interface Task<T> {
        Request request();
        void executeAsync();
        T execute() throws Exception;
        Object tag();
        boolean isCanceled();
        boolean isExecuted();
        void cancel();
    }
}
