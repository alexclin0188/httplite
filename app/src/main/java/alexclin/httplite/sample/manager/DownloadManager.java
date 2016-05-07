package alexclin.httplite.sample.manager;

import android.content.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import alexclin.httplite.sample.adapter.DownloadAdapter;

/**
 * DownloadManager
 *
 * @author alexclin 16/1/10 15:21
 */
public class DownloadManager {
    private static HashMap<String,DownloadTask> mTaskMap = new HashMap<>();
    private static List<DownloadTask> mTaskList = new ArrayList<>();

    private static DownloadListener mDownloadListener;

    private static DownloadAdapter mDownloadAdapter;

    public static void setDownloadListener(DownloadListener listener){
        DownloadManager.mDownloadListener = listener;
    }

    public static void download(Context ctx,String url,String dir,String name,String hash){
        init();
        if(!mTaskMap.containsKey(url)){
            DownloadTask task = new DownloadTask(url,name,dir,hash);
            task.start(ctx);
            mTaskMap.put(url, task);
            mTaskList.add(task);
            if(mDownloadListener!=null){
                mDownloadListener.onTaskChanged(mTaskList);
            }
        }
    }

    private static void init(){
        if(mDownloadAdapter == null){
            mDownloadAdapter = new DownloadAdapter(mTaskList);
            mDownloadListener = mDownloadAdapter;
        }
    }

    public static DownloadAdapter getDownloadAdapter(){
        init();
        return mDownloadAdapter;
    }

    public interface DownloadListener{
        void onTaskChanged(List<DownloadTask> taskList);
    }

}
