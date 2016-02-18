package alexclin.httplite.url;

import android.os.Process;
import android.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.url.cache.URLCache;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * CacheDispatcher
 *
 * @author alexclin
 * @date 16/2/18 19:42
 */
public class CacheDispatcher extends Thread implements Dispatcher{

    private final PriorityBlockingQueue<Task> mCacheQueue = new PriorityBlockingQueue<>();

    private final Map<String, Queue<Pair<Task,Boolean>>> mWaitingRequests = new HashMap<>();

    private NetworkDispatcher networkDispatcher;

    private URLCache cache;

    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

    public CacheDispatcher(NetworkDispatcher networkDispatcher,URLCache cache) {
        this.networkDispatcher = networkDispatcher;
        this.cache = cache;
    }

    public void dispatch(Task task) {
        if(!isSameKeyTaskRunning(task,false)){
            mCacheQueue.add(task);
        }
    }

    public Response execute(Task task) throws Exception{
        if(!isSameKeyTaskRunning(task,true)){
            try {
                return networkDispatcher.execute(task);
            } finally {
                notifyTaskFinish(getCacheKey(task.request()));
            }
        }else{
            task.wait();
        }
        Response response = cache.get(task.request());
        if(response==null){
            response = networkDispatcher.execute(task);
        }
        return response;
    }

    @Override
    public void cancel(Object tag) {
        //TODO
    }

    public Response cacheResponse(Response response) {
        String cacheKey = getCacheKey(response.request());
        if(cacheKey!=null){
            try {
                cache.put(response);
                response = cache.get(response.request());
            } catch (IOException e) {
                e.printStackTrace();
            }
            notifyTaskFinish(cacheKey);
        }
        return response;
    }

    private void notifyTaskFinish(String cacheKey) {
        synchronized (mWaitingRequests) {
            Queue<Pair<Task,Boolean>> waitingRequests = mWaitingRequests.remove(cacheKey);
            if (waitingRequests != null) {
                LogUtil.i(String.format("Releasing %d waiting requests for cacheKey=%s.",
                        waitingRequests.size(), cacheKey));
                // Process all queued up requests. They won't be considered as in flight, but
                // that's not a problem as the cache has been primed by 'request'.
                for(Pair<Task,Boolean> pair:waitingRequests) {
                    if(pair.second){
                        pair.first.notify();
                    }else{
                        mCacheQueue.add(pair.first);
                    }
                }
            }
        }
    }

    private String getCacheKey(Request request){
        return Util.md5Hex(request.getUrl());
    }

    private boolean isSameKeyTaskRunning(Task task,boolean sync){
        synchronized (mWaitingRequests) {
            String cacheKey = getCacheKey(task.request());
            if (mWaitingRequests.containsKey(cacheKey)) {
                // There is already a request in flight. Queue up.
                Queue<Pair<Task,Boolean>> stagedRequests = mWaitingRequests.get(cacheKey);
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<Pair<Task,Boolean>>();
                }
                stagedRequests.add(new Pair<Task, Boolean>(task,sync));
                mWaitingRequests.put(cacheKey, stagedRequests);
                LogUtil.i(String.format("Request for cacheKey=%s is in flight, putting on hold.", cacheKey));
                return true;
            } else {
                // Insert 'null' queue for this cacheKey, indicating there is now a request in
                // flight.
                mWaitingRequests.put(cacheKey, null);
                return false;
            }
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true){
            final Task task;
            try {
                // Take a request from the queue.
                task = mCacheQueue.take();
            } catch (InterruptedException e) {
                // We may have been interrupted because it was time to quit.
                if (mQuit) {
                    return;
                }
                continue;
            }
            try {
                final Response response = cache.get(task.request());
                if(response==null){
                    networkDispatcher.dispatch(task);
                    continue;
                }
                networkDispatcher.getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        ((URLTask)task).onResponse(response);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
