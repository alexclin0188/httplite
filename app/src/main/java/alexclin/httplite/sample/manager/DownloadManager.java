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
    private HashMap<String,DownloadTask> mTaskMap = new HashMap<>();
    private List<DownloadTask> mTaskList = new ArrayList<>();

    private DownloadAdapter mDownloadAdapter;


    public void download(Context ctx,String url,String dir,String name,String hash){
        init();
        DownloadTask task = addTask(url,dir,name,hash);
        if(task!=null) task.start(ctx);
    }

    public DownloadTask addTask(String url,String dir,String name,String hash){
        if(!mTaskMap.containsKey(url)){
            DownloadTask task = new DownloadTask(url,name,dir,hash);
            mTaskMap.put(url, task);
            mTaskList.add(task);
            mDownloadAdapter.notifyDataSetChanged();
            return task;
        }
        return null;
    }

    public DownloadTask addTask(String url,String dir,String name){
        return addTask(url,dir,name,null);
    }

    private void init(){
        if(mDownloadAdapter == null){
            mDownloadAdapter = new DownloadAdapter(mTaskList);
        }
    }

    public DownloadAdapter getDownloadAdapter(){
        init();
        return mDownloadAdapter;
    }

    public void release() {
        for(DownloadTask task:mTaskList){
            task.cancel();
            task.setStateListener(null);
        }
        mTaskList.clear();
        mTaskMap.clear();
    }

    public void stopAll(){
        for(DownloadTask task:mTaskList){
            task.cancel();
        }
    }
}
