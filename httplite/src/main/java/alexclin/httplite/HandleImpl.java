package alexclin.httplite;

import alexclin.httplite.exception.IllegalOperationException;

/**
 * @author xiehonglin429 on 2017/3/4.
 */

class HandleImpl implements Handle{
    private volatile boolean isCanceled;
    private volatile boolean isExecuted;
    private Handle attched;

    @Override
    public void cancel() {
        isCanceled = true;
        if(attched!=null) attched.cancel();
    }

    @Override
    public boolean isCanceled() {
        return isCanceled||(attched!=null&&attched.isCanceled());
    }

    @Override
    public boolean isExecuted() {
        return isExecuted&&attched!=null&&attched.isExecuted();
    }

    @Override
    public void setHandle(Handle handle) {
        if(this.attched!=null){
            throw new IllegalOperationException("setHandle show called just once");
        }
        this.attched = handle;
    }
}
