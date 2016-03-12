package alexclin.httplite.internal;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import alexclin.httplite.util.Util;

/**
 * TaskDispatcher
 *
 * @author alexclin
 * @date 16/1/2 19:19
 */
public class TaskDispatcher<T> implements Dispatcher<T> {

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

    private final Deque<Task> executingSyncCalls = new ArrayDeque<>();

    public TaskDispatcher() {
        this(null);
    }

    public TaskDispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public void dispatch(Task<T> task) {
        dispatchInner(new AsyncWrapper(task, this));
    }

    @Override
    public T execute(Task<T> task) throws Exception{
        registerSyncCall(task);
        try {
            return task.execute();
        } finally {
            unregisterSyncCall(task);
        }
    }

    public synchronized ExecutorService getExecutorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("URLite Dispatcher", false));
        }
        return executorService;
    }

    void registerSyncCall(Task task){
        executingSyncCalls.offer(task);
    }

    void unregisterSyncCall(Task task) {
        executingSyncCalls.remove(task);
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
            }
        }

        for (AsyncWrapper call : runningCalls) {
            if (Util.equal(tag, call.realTask.tag())) {
                call.realTask.cancel();
            }
        }

        for (Task call : executingSyncCalls) {
            if (Util.equal(tag, call.tag())) {
                call.cancel();
            }
        }
    }

    @Override
    public void cancelAll() {
        for (AsyncWrapper call : readyCalls) {
            call.realTask.cancel();
        }

        for (AsyncWrapper call : runningCalls) {
            call.realTask.cancel();
        }

        for (Task call : executingSyncCalls) {
            call.cancel();
        }
    }

    @Override
    public void shutdown() {
        if(executorService!=null){
            executorService.shutdown();
        }
    }

    public synchronized void dispatchInner(AsyncWrapper wrapper) {
        if (runningCalls.size() < maxRequests) {
            runningCalls.add(wrapper);
            if(getExecutorService().isShutdown()) return;
            getExecutorService().submit(wrapper);
        } else {
            readyCalls.add(wrapper);
        }
    }

    public synchronized void onFinished(AsyncWrapper wrapper) {
        runningCalls.remove(wrapper);
        do {
            if(getExecutorService().isShutdown()) return;
            wrapper = readyCalls.poll();
            if(wrapper!=null&&!wrapper.realTask.isCanceled()&&!wrapper.realTask.isExecuted()){
                dispatchInner(wrapper);
                return;
            }
        }while (wrapper!=null);
    }

    private static class AsyncWrapper implements Runnable {
        private Task realTask;
        private TaskDispatcher dispatcher;

        public AsyncWrapper(Task realTask,TaskDispatcher dispatcher) {
            this.realTask = realTask;
            this.dispatcher = dispatcher;
        }

        @Override
        public void run() {
            realTask.executeAsync();
            final TaskDispatcher dispatcher = this.dispatcher;
            if(dispatcher!=null)dispatcher.onFinished(this);
            this.dispatcher = null;
        }
    }
}
