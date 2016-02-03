package alexclin.httplite.urlconnection;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.HttpLite;
import alexclin.httplite.Response;
import alexclin.httplite.util.Util;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/2 19:19
 */
public class Dispatcther {

    private int maxRequests = 64;

    /**
     * Executes calls. Created lazily.
     */
    private ExecutorService executorService;

    /**
     * Ready calls in the order they'll be run.
     */
    private final Deque<AsyncWrapper> readyCalls = new ArrayDeque<>();

    /**
     * Running calls. Includes canceled calls that haven't finished yet.
     */
    private final Deque<AsyncWrapper> runningCalls = new ArrayDeque<>();

    private final Deque<Task> executedCalls = new ArrayDeque<>();

    public Dispatcther() {
    }

    public Dispatcther(ExecutorService executorService) {
        this.executorService = executorService;
    }

    void dispatch(Task task) {
        dispatchInnerMain(new AsyncWrapper(task));
    }

    Response execute(Task task) throws Exception{
        executedCalls.offer(task);
        Response response = task.execute();
        executedCalls.remove(task);
        return response;
    }

    synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("URLite Dispatcher", false));
        }
        return executorService;
    }

    /**
     * Set the maximum number of requests to execute concurrently. Above this requests queue in
     * memory, waiting for the running calls to complete.
     * <p>
     * <p>If more than {@code maxRequests} requests are in flight when this is invoked, those requests
     * will remain in flight.
     */
    public synchronized void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        this.maxRequests = maxRequests;
        if (runningCalls.size() >= maxRequests) return;
        if (readyCalls.isEmpty()) return; // No ready calls to promote.
        for (Iterator<AsyncWrapper> i = readyCalls.iterator(); i.hasNext(); ) {
            AsyncWrapper call = i.next();
            i.remove();
            runningCalls.add(call);
            getExecutorService().execute(call);
            if (runningCalls.size() >= maxRequests) return;
        }
    }

    public synchronized int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Cancel all calls with the tag {@code tag}.
     */
    public synchronized void cancel(Object tag) {
        for (AsyncWrapper call : readyCalls) {
            if (Util.equal(tag, call.realTask.tag())) {
                call.realTask.cancel();
                readyCalls.remove(call);
            }
        }

        for (AsyncWrapper call : runningCalls) {
            if (Util.equal(tag, call.realTask.tag())) {
                call.realTask.cancel();
            }
        }

        for (Task call : executedCalls) {
            if (Util.equal(tag, call.tag())) {
                call.cancel();
                executedCalls.remove(call);
            }
        }
    }

    public void dispatchInner(AsyncWrapper wrapper) {
        if (runningCalls.size() < maxRequests) {
            runningCalls.add(wrapper);
            getExecutorService().submit(wrapper);
        } else {
            readyCalls.add(wrapper);
        }
    }

    public void onFinished(AsyncWrapper wrapper) {
        runningCalls.remove(wrapper);
        do {
            wrapper = readyCalls.poll();
            if(wrapper!=null&&!wrapper.realTask.isCanceled()&&!wrapper.realTask.isExecuted()){
                dispatchInner(wrapper);
                return;
            }
        }while (wrapper!=null);
    }

    public void dispatchInnerMain(final AsyncWrapper wrapper) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                dispatchInner(wrapper);
            }
        });
    }

    public void onFinishedMain(final AsyncWrapper wrapper) {
        HttpLite.runOnMainThread(new Runnable() {
            @Override
            public void run() {
                onFinished(wrapper);
            }
        });
    }

    private class AsyncWrapper implements Runnable {
        private Task realTask;

        public AsyncWrapper(Task realTask) {
            this.realTask = realTask;
        }

        @Override
        public void run() {
            realTask.executeAsync();
            onFinishedMain(this);
        }
    }
}
