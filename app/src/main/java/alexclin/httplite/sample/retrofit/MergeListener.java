package alexclin.httplite.sample.retrofit;

import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.LogUtil;

/**
 * MergeListener
 *
 * @author alexclin 16/1/30 19:00
 */
public class MergeListener implements ProgressListener,RetryListener {

    @Override
    public void onProgressUpdate(long current, long total) {
        LogUtil.e("current:"+current+",total:"+total);
    }

    @Override
    public void onRetry(int count, int maxCount) {
        LogUtil.e("count:"+count+",maxCount:"+maxCount);
    }
}
