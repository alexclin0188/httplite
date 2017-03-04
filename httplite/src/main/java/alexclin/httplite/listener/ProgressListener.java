package alexclin.httplite.listener;

/**
 * ProgressListener
 *
 * @author alexclin 16/1/1 21:42
 */
public interface ProgressListener {
    void onProgress(boolean out, long current, long total);
}
