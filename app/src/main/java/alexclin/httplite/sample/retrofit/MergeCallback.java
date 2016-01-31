package alexclin.httplite.sample.retrofit;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.CancelListener;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.listener.RetryListener;
import alexclin.httplite.util.LogUtil;

/**
 * MergeCallback
 *
 * @author alexclin
 * @date 16/1/31 15:31
 */
public class MergeCallback<T> implements Callback<T>,ProgressListener,RetryListener,CancelListener {
    @Override
    public void onSuccess(T result, Map<String, List<String>> headers) {
        LogUtil.e("Result:"+result);
    }

    @Override
    public void onFailed(Request req, Exception e) {
        LogUtil.e("onFailed:",e);
    }

    @Override
    public void onCancel(Request request) {
        LogUtil.e("onCancel:");
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
