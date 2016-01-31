package alexclin.httplite.listener;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 21:43
 */
public interface RetryListener {
    void onRetry(int count,int maxCount);
}
