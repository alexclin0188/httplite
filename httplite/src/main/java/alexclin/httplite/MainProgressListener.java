package alexclin.httplite;

import alexclin.httplite.listener.ProgressListener;

/**
 * MainProgressListener
 *
 * @author alexclin  16/3/31 22:52
 */
class MainProgressListener implements ProgressListener{
    private ProgressListener listener;

    public MainProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    @Override
    public void onProgressUpdate(final boolean out,final long current,final long total) {
        HttpLite.postOnMain(new Runnable() {
            @Override
            public void run() {
                listener.onProgressUpdate(out,current,total);
            }
        });
    }
}
