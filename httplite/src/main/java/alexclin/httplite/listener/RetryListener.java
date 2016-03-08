package alexclin.httplite.listener;

/**
 * RetryListener
 *
 * @author alexclin 16/1/1 21:43
 */
public interface RetryListener {
    void onRetry(int count,int maxCount);
}
