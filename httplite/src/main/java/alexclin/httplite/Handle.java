package alexclin.httplite;

/**
 * Handle
 *
 * @author alexclin
 * @date 16/2/17 19:20
 */
public interface Handle {
    Request request();
    void cancel();
    boolean isExecuted();
    boolean isCanceled();
}
