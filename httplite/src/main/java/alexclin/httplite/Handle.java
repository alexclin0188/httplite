package alexclin.httplite;

/**
 * Handle
 *
 * @author alexclin  16/5/7 21:44
 */
public interface Handle {
    Request request();

    void cancel();

    boolean isCanceled();

    boolean isExecuted();
}
