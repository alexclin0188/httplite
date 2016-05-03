package alexclin.httplite.sample.manager;

import com.example.util.EncryptUtil;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.util.LogUtil;

/**
 * DownloadTask
 *
 * @author alexclin 16/1/10 15:48
 */
public class DownloadTask implements Callback<File>,ProgressListener {
    private TaskStateListener stateListener;

    private long total;
    private long current;
    private boolean isFinished;
    private boolean isCanceled;
    private boolean isFailed;
    private String hash;
    private String realHash;
    private String name;
    private String path;
    private File file;
    private Map<String, List<String>> headers;
    public DownloadTask(String name,String path,String hash) {
        this.hash = hash;
        this.name = name;
        this.path = path;
    }

    @Override
    public void onSuccess(File result, Map<String, List<String>> headers) {
        isFinished = true;
        this.headers = headers;
        this.file = result;
        realHash = EncryptUtil.hash(result);
        if(stateListener!=null){
            stateListener.onStateChanged(this);
        }
        LogUtil.e("OnSuccess:"+result);
        LogUtil.e("OnSuccess hash:"+isValidHash());
    }

    @Override
    public void onFailed(Request req, Exception e) {
        isFailed = true;
        if(stateListener!=null){
            stateListener.onStateChanged(this);
        }
        LogUtil.e("onFailed:"+e);
        e.printStackTrace();
    }

    @Override
    public void onProgressUpdate(boolean out,long current, long total) {
        this.total = total;
        this.current = current;
        if(stateListener!=null){
            stateListener.onProgressUpdate(current, total);
        }
        LogUtil.e(String.format("onProgressUpdate:%d,%d",current,total));
    }

    public void resume(){
        if(isFinished){
            return;
        }
        isCanceled = false;
        isFailed = false;
        //TODO handle.resume();
    }

    public long getTotal() {
        return total;
    }

    public long getCurrent() {
        return current;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public boolean isCanceled() {
        return isCanceled;
    }

    public boolean isFailed() {
        return isFailed;
    }

    public File getFile() {
        return file;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

//    public void setHandle(Handle handle) {
//        this.handle = handle;
//    }

    public boolean isValidHash(){
        return isFinished&&(hash.equals(realHash));
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setStateListener(TaskStateListener stateListener) {
        this.stateListener = stateListener;
    }

    public interface TaskStateListener {
        void onProgressUpdate(long current,long total);
        void onStateChanged(DownloadTask task);
    }
}
