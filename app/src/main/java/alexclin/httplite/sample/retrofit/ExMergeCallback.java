package alexclin.httplite.sample.retrofit;

import java.io.File;

/**
 * ExMergeCallback
 *
 * @author alexclin 16/1/31 17:23
 */
public class ExMergeCallback extends MergeCallback<File> {
    private boolean isCanceled;

    @Override
    public void onProgress(boolean out, long current, long total) {
        super.onProgress(out,current, total);
    }
}
