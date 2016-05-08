package alexclin.httplite.sample.manager;

/**
 * TaskStateListener
 *
 * @author alexclin  16/5/7 17:24
 */
public interface TaskStateListener {
    void onProgressUpdate(long current,long total);
    void onStateChanged(DownloadTask task);
}
