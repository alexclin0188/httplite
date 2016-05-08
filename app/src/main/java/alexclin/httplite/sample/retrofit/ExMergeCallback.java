package alexclin.httplite.sample.retrofit;

import java.io.File;

import alexclin.httplite.util.LogUtil;

/**
 * ExMergeCallback
 *
 * @author alexclin 16/1/31 17:23
 */
public class ExMergeCallback extends MergeCallback<File> {
    private boolean isCanceled;

    @Override
    public void onProgressUpdate(boolean out,long current, long total) {
        super.onProgressUpdate(out,current, total);
    }
}
