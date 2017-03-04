package alexclin.httplite.impl;

import alexclin.httplite.HttpLite;
import alexclin.httplite.listener.ProgressListener;

/**
 * ProgressRunnable
 *
 * @author alexclin  16/3/31 23:15
 */
public class ProgressRunnable implements Runnable {
    private static final int GAP = 1000;
    public interface ProgressSource extends ProgressListener{
        long progress();
    }

    private final long total;
    private final ProgressSource source;
    private final boolean isOut;
    private volatile boolean loop = true;

    public ProgressRunnable(boolean isOut, long total, ProgressSource source) {
        this.isOut = isOut;
        this.total = total;
        this.source = source;
    }

    @Override
    public void run() {
        source.onProgress(isOut,source.progress(),total);
        postDelaySelf();
    }

    private void postDelaySelf(){
        if(loop) HttpLite.mainHandler().postDelayed(this,GAP);
    }

    public void end(){
        HttpLite.mainHandler().removeCallbacks(this);
        loop = false;
        HttpLite.mainHandler().post(this);
    }
}
