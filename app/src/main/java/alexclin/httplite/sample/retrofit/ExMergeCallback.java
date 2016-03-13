package alexclin.httplite.sample.retrofit;

import java.io.File;

import alexclin.httplite.Handle;
import alexclin.httplite.util.LogUtil;

/**
 * ExMergeCallback
 *
 * @author alexclin 16/1/31 17:23
 */
public class ExMergeCallback extends MergeCallback<File> {
    private boolean isCanceled;
    private Handle handle;

    public void setHandle(Handle handle) {
        this.handle = handle;
    }

    @Override
    public void onProgressUpdate(long current, long total) {
        super.onProgressUpdate(current, total);
        if(!isCanceled){
            isCanceled = true;
            LogUtil.e("Cancel:"+handle);
            if(handle!=null) handle.cancel();
        }
    }
}
