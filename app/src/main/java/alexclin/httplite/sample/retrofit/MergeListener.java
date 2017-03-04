package alexclin.httplite.sample.retrofit;

import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.util.LogUtil;

/**
 * MergeListener
 *
 * @author alexclin 16/1/30 19:00
 */
public class MergeListener implements ProgressListener {

    @Override
    public void onProgress(boolean out, long current, long total) {
        LogUtil.e("current:"+current+",total:"+total);
    }
}
