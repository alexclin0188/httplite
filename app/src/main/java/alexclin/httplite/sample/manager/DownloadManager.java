package alexclin.httplite.sample.manager;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Request;
import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.ProgressListener;
import alexclin.httplite.sample.App;
import alexclin.httplite.sample.adapter.DownloadAdpater;

/**
 * DownloadManager
 *
 * @author alexclin 16/1/10 15:21
 */
public class DownloadManager {
    private static HashMap<String,DownloadTask> mTaskMap = new HashMap<>();
    private static List<DownloadTask> mTaskList = new ArrayList<>();

    private static DownloadListener mDownloadListener;

    private static DownloadAdpater mDownloadAdapter;

    public static void setDownloadListener(DownloadListener listener){
        DownloadManager.mDownloadListener = listener;
    }

    public static void download(Context ctx,String url,String dir,String name,String hash){
        init();
        if(!mTaskMap.containsKey(url)){
            DownloadTask task = new DownloadTask(name,dir,hash);
            App.httpLite(ctx).url(url).onProgress(task).intoFile(dir,name,true,true).download(task);
            //TODO task.setHandle();
            mTaskMap.put(url, task);
            mTaskList.add(task);
            if(mDownloadListener!=null){
                mDownloadListener.onTaskChanged(mTaskList);
            }
        }
        HttpLite mHttpLite = App.httpLite(ctx);
        mHttpLite.url(url).intoFile(dir,name,true,true)
                .onProgress(new ProgressListener() {
                    @Override
                    public void onProgressUpdate(boolean out, long current, long total) {
                        //TODO
                    }
                })
                .download(new Callback<File>() {
                    @Override
                    public void onSuccess(Request req, Map<String, List<String>> headers, File result) {
                        //TODO
                    }

                    @Override
                    public void onFailed(Request req, Exception e) {
                        //TODO
                    }
                });
    }

    private static void init(){
        if(mDownloadAdapter == null){
            mDownloadAdapter = new DownloadAdpater(mTaskList);
            mDownloadListener = mDownloadAdapter;
        }
    }

    public static DownloadAdpater getDownloadAdapter(){
        init();
        return mDownloadAdapter;
    }

    public interface DownloadListener{
        void onTaskChanged(List<DownloadTask> taskList);
    }

}
