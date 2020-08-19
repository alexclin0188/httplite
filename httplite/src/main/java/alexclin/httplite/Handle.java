package alexclin.httplite;

/**
 * Handle
 *
 * @author alexclin  16/5/7 21:44
 */
public interface Handle {
    void cancel();

    boolean isCanceled();

    boolean isExecuted();

    void setHandle(Handle handle);
}
