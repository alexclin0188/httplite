package alexclin.httplite.url;

import android.os.Process;
import android.util.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import alexclin.httplite.Request;
import alexclin.httplite.listener.Response;
import alexclin.httplite.exception.HttpException;
import alexclin.httplite.url.cache.CacheImpl;
import alexclin.httplite.url.cache.CacheHandler;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * CacheDispatcher
 *
 * @author alexclin 16/2/18 19:42
 */
class CacheDispatcher extends Thread implements Dispatcher{

    private final PriorityBlockingQueue<Task> mCacheQueue = new PriorityBlockingQueue<>();

    private final Map<String, Queue<Pair<Task,Boolean>>> mWaitingRequests = new HashMap<>();

    private NetDispatcher networkDispatcher;

    private CacheImpl cache;

    /** Used for telling us to die. */
    private volatile boolean mQuit = false;

    CacheDispatcher(NetDispatcher networkDispatcher, CacheImpl cache) {
        this.networkDispatcher = networkDispatcher;
        this.cache = cache;
        start();
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
                notifyTaskFinish(cache.createCacheKey(task.request()));
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
        for(Queue<Pair<Task,Boolean>> queue:mWaitingRequests.values()){
            for(Pair<Task,Boolean> pair:queue){
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
        for(Queue<Pair<Task,Boolean>> queue:mWaitingRequests.values()){
            for(Pair<Task,Boolean> pair:queue){
                Task task = pair.first;
                task.cancel();
            }
        }
    }

    @Override
    public void shutdown() {
        mQuit = true;
    }

    Response cacheResponse(Response response) throws IOException {
        String cacheKey = cache.createCacheKey(response.request());
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
            Queue<Pair<Task,Boolean>> waitingRequests = mWaitingRequests.remove(cacheKey);
            if (waitingRequests != null) {
                LogUtil.i(String.format(Locale.ENGLISH,"Releasing %d waiting requests for cacheKey=%s.",
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

    private boolean isSameKeyTaskRunning(Task task,boolean sync){
        synchronized (mWaitingRequests) {
            String cacheKey = cache.createCacheKey(task.request());
            if (mWaitingRequests.containsKey(cacheKey)) {
                // There is already a request in flight. Queue up.
                Queue<Pair<Task,Boolean>> stagedRequests = mWaitingRequests.get(cacheKey);
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

    void addCacheHeaders(Request request,Map<String, List<String>> headers){
        cache.addCacheHeaders(request,headers);
    }

    boolean canCache(Request request) {
        return cache.canCache(request)&&request.getCacheExpiredTime()!=Request.NO_CACHE;
    }

    private static class CacheTask implements Task{
        private Task task;
        private Response response;

        CacheTask(Task task,Response response) {
            this.task = task;
            this.response = response;
        }

        @Override
        public Request request() {
            return task.request();
        }

        @Override
        public void executeCallback(URLite lite) {
            ((URLTask)task).onResponse(response);
        }

        @Override
        public Response execute(URLite lite) throws Exception {
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

    static class DefaultCachePolicy implements CacheHandler {

        @Override
        public String createCacheKey(Request request) {
            return Util.md5Hex(request.getUrl());
        }

        @Override
        public boolean canCache(Request request) {
            return request.getMethod()== Request.Method.GET;
        }
    }
}
