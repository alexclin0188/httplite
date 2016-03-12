package alexclin.httplite.sample.retrofit;

import alexclin.httplite.Request;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.LogUtil;

/**
 * MergeListener
 *
 * @author alexclin 16/1/30 19:00
 */
public class MergeListener implements ProgressListener,RetryListener,CancelListener {
    @Override
    public void onCancel(Request request) {
        LogUtil.e("Request:"+request);
    }

    @Override
    public void onProgressUpdate(long current, long total) {
        LogUtil.e("current:"+current+",total:"+total);
    }

    @Override
    public void onRetry(int count, int maxCount) {
        LogUtil.e("count:"+count+",maxCount:"+maxCount);
    }
}
