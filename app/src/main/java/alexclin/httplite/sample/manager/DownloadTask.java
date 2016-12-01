package alexclin.httplite.sample.manager;

import android.content.Context;

import com.example.util.EncryptUtil;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import alexclin.httplite.Handle;
import alexclin.httplite.Request;
import alexclin.httplite.exception.CanceledException;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.sample.App;
import alexclin.httplite.util.LogUtil;

/**
 * DownloadTask
 *
 * @author alexclin 16/1/10 15:48
 */
public class DownloadTask implements Callback<File>,ProgressListener {

    public enum State{
        IDLE(true),Downloading(false),Failed(true),Canceled(true),Finished(false);

        public final boolean startAble;

        State(boolean startAble) {
            this.startAble = startAble;
        }
    }

    private TaskStateListener stateListener;

    private State mState;
    private long total;
    private long current;
    private String hash;
    private String realHash;
    private String name;
    private String url;
    private String path;
    private File file;
    private Handle mRequestHandle;

    public DownloadTask(String url,String name,String path,String hash) {
        this.hash = hash;
        this.name = name;
        this.path = path;
        this.url = url;
        this.mState = State.IDLE;
    }

    public DownloadTask(String url,String name,String path) {
        this(url,name,path,null);
    }

    @Override
    public void onSuccess(Request req,Map<String, List<String>> headers,File result) {
        this.file = result;
        realHash = EncryptUtil.hash(result);
        updateState(State.Finished);
    }

    @Override
    public void onFailed(Request req, Exception e) {
        State state = (e instanceof CanceledException)?State.Canceled:State.Failed;
        updateState(state);
        LogUtil.e("onFailed:",e);
    }

    @Override
    public void onProgressUpdate(boolean out,long current, long total) {
        current = current/1024;
        total = total/1024;
        updateProgress(current, total);
    }

    public void resume(Context context){
        if(!mState.startAble){
            return;
        }
        start(context);
    }

    public void start(Context ctx){
        if(!mState.startAble){
            return;
        }
        current = 0;
        total = 0;
        mRequestHandle = App.httpLite(ctx).url(url).onProgress(this).intoFile(path,name,true,true).get().build().call().async(this);
        updateState(State.Downloading);
    }

    public long getTotal() {
        return total;
    }

    public long getCurrent() {
        return current;
    }

    public File getFile() {
        return file;
    }

    public String hashInfo(){
        return String.format(Locale.getDefault(),"realHash:%s,serverHash:%s",realHash,hash);
    }

    public String getPath() {
        if(mRequestHandle !=null) return mRequestHandle.request().getDownloadParams().getTargetFile().getAbsolutePath();
        return path+name;
    }

    public String getName() {
        if(mRequestHandle !=null) return mRequestHandle.request().getDownloadParams().getTargetFile().getName();
        return name;
    }

    public State getState() {
        return mState;
    }

    private void updateState(State state){
        mState = state;
        if(stateListener!=null) stateListener.onStateChanged(this);
    }

    private void updateProgress(long current,long total){
        this.total = total;
        this.current = current;
        if(mState!=State.Downloading) updateState(State.Downloading);
        if(stateListener!=null) stateListener.onProgressUpdate(current,total);
    }

    public void setStateListener(TaskStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public void cancel() {
        if(mRequestHandle !=null) mRequestHandle.cancel();
    }
}
