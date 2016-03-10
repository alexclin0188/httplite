package alexclin.httplite.url;

import alexclin.httplite.Response;

/**
 * Dispatcher
 *
 * @author alexclin 16/2/18 19:53
 */
public interface Dispatcher {
    void dispatch(Task task);

    Response execute(Task task) throws Exception;

    void cancel(Object tag);
}
