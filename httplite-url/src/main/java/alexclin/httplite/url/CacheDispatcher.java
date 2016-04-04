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
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.mock.Dispatcher;
import alexclin.httplite.mock.TaskDispatcher;
import alexclin.httplite.url.cache.CacheImpl;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * CacheDispatcher
 *
 * @author alexclin 16/2/18 19:42
 */
public class CacheDispatcher extends Thread implements Dispatcher<Response>{

    private final PriorityBlockingQueue<Task<Response>> mCacheQueue = new PriorityBlockingQueue<>();

    private final Map<String, Queue<Pair<Task<Response>,Boolean>>> mWaitingRequests = new HashMap<>();

    private TaskDispatcher<Response> networkDispatcher;

    private CacheImpl cache;

    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

    public CacheDispatcher(TaskDispatcher<Response> networkDispatcher,CacheImpl cache) {
        this.networkDispatcher = networkDispatcher;
        this.cache = cache;
        start();
    }

    public void dispatch(Task<Response> task) {
        if(!isSameKeyTaskRunning(task,false)){
            mCacheQueue.add(task);
        }
    }

    public Response execute(Task<Response> task) throws Exception{
        if(!isSameKeyTaskRunning(task,true)){
            try {
                return networkDispatcher.execute(task);
            } finally {
                notifyTaskFinish(getCacheKey(task.request()));
            }
        }else{
            task.wait();
        }
        Response response = cache.get(task.request(),false);
        if(response==null){
            response = networkDispatcher.execute(task);
        }
        return response;
    }

    @Override
    public void cancel(Object tag) {
        for(Task task:mCacheQueue){
            if(Util.equal(tag,task.tag())) task.cancel();
        }
        for(Queue<Pair<Task<Response>,Boolean>> queue:mWaitingRequests.values()){
            for(Pair<Task<Response>,Boolean> pair:queue){
                Task task = pair.first;
                if(Util.equal(tag,task.tag())) task.cancel();
            }
        }
    }

    @Override
    public void cancelAll() {
        for(Task task:mCacheQueue){
            task.cancel();
        }
        for(Queue<Pair<Task<Response>,Boolean>> queue:mWaitingRequests.values()){
            for(Pair<Task<Response>,Boolean> pair:queue){
                Task task = pair.first;
                task.cancel();
            }
        }
    }

    @Override
    public void shutdown() {
        mQuit = true;
    }

    public Response cacheResponse(Response response) throws IOException {
        String cacheKey = getCacheKey(response.request());
        if (cacheKey != null) {
            try {
                if (response.code() < 300) {
                    LogUtil.i("success response, put cache");
                    response = cache.put(response);
                } else if (response.code() == HttpException.SC_NOT_MODIFIED) {
                    LogUtil.i("NOT_MODIFIED, use old cache");
                    return cache.get(response.request(),true);
                } else {
                    LogUtil.i("error response, clear cache");
                    cache.remove(response.request());
                }
            } finally {
                notifyTaskFinish(cacheKey);
            }
        }
        return response;
    }

    private void notifyTaskFinish(String cacheKey) {
        synchronized (mWaitingRequests) {
            Queue<Pair<Task<Response>,Boolean>> waitingRequests = mWaitingRequests.remove(cacheKey);
            if (waitingRequests != null) {
                LogUtil.i(String.format("Releasing %d waiting requests for cacheKey=%s.",
                        waitingRequests.size(), cacheKey));
                // Process all queued up requests. They won't be considered as in flight, but
                // that's not a problem as the cache has been primed by 'request'.
                for(Pair<Task<Response>,Boolean> pair:waitingRequests) {
                    if(pair.second){
                        pair.first.notify();
                    }else{
                        mCacheQueue.add(pair.first);
                    }
                }
            }
        }
    }

    public static String getCacheKey(Request request){
        return Util.md5Hex(request.getUrl());
    }

    private boolean isSameKeyTaskRunning(Task<Response> task,boolean sync){
        synchronized (mWaitingRequests) {
            String cacheKey = getCacheKey(task.request());
            if (mWaitingRequests.containsKey(cacheKey)) {
                // There is already a request in flight. Queue up.
                Queue<Pair<Task<Response>,Boolean>> stagedRequests = mWaitingRequests.get(cacheKey);
                if (stagedRequests == null) {
                    stagedRequests = new LinkedList<>();
                }
                stagedRequests.add(new Pair<>(task,sync));
                mWaitingRequests.put(cacheKey, stagedRequests);
                LogUtil.i(String.format("Request for cacheKey=%s is in flight, putting on hold.", cacheKey));
                return true;
            } else {
                mWaitingRequests.put(cacheKey, null);
                return false;
            }
        }
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true){
            final Task<Response> task;
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
                final Response response = cache.get(task.request(),false);
                if(response==null){
                    LogUtil.i("No cache, use net");
                    networkDispatcher.dispatch(task);
                    continue;
                }
                LogUtil.i("cache hit, dispatch to parse result");
                networkDispatcher.dispatch(new CacheTask(task,response));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void addCacheHeaders(Request request){
        cache.addCacheHeaders(request);
    }

    static class CacheTask implements Task<Response>{
        private Task task;
        private Response response;

        public CacheTask(Task task,Response response) {
            this.task = task;
            this.response = response;
        }

        @Override
        public Request request() {
            return task.request();
        }

        @Override
        public void executeAsync() {
            ((URLTask)task).onResponse(response);
        }

        @Override
        public Response execute() throws Exception {
            return response;
        }

        @Override
        public Object tag() {
            return task.tag();
        }

        @Override
        public boolean isCanceled() {
            return task.isCanceled();
        }

        @Override
        public boolean isExecuted() {
            return true;
        }

        @Override
        public void cancel() {
            task.cancel();
        }
    }
}
