package alexclin.httplite.listener;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 21:42
 */
public interface ProgressListener {
    void onProgressUpdate(long current,long total);
}
