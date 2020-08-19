package alexclin.httplite.sample.retrofit;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.util.LogUtil;

/**
 * MergeCallback
 *
 * @author alexclin 16/1/31 15:31
 */
public class MergeCallback<T> implements Callback<T>,ProgressListener {
    @Override
    public void onSuccess(Request req, Map<String, List<String>> headers,T result) {
        LogUtil.e("BaseResult:"+result);
    }

    @Override
    public void onFailed(Request req, Exception e) {
        LogUtil.e("onFailed:",e);
    }

    @Override
    public void onProgress(boolean out, long current, long total) {
        LogUtil.e("current:"+current+",total:"+total);
    }
}
