package alexclin.httplite;

import alexclin.httplite.exception.IllegalOperationException;

/**
 * @author xiehonglin429 on 2017/3/4.
 */

class HandleImpl implements Handle{
    private volatile boolean isCanceled;
    private volatile boolean isExecuted;
    private Handle attached;

    @Override
    public void cancel() {
        isCanceled = true;
        if(attached !=null) attached.cancel();
    }

    @Override
    public boolean isCanceled() {
        return isCanceled||(attached !=null&& attached.isCanceled());
    }

    @Override
    public boolean isExecuted() {
        return isExecuted&& attached !=null&& attached.isExecuted();
    }

    @Override
    public void setHandle(Handle handle) {
        if(this.attached !=null){
            throw new IllegalOperationException("setHandle show called just once");
        }
        this.attached = handle;
    }

    void setExecuted(){
        isExecuted = true;
    }
}
