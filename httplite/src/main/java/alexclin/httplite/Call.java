package alexclin.httplite;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.Clazz;
import alexclin.httplite.util.Result;
import alexclin.httplite.util.Util;

/**
 * Call
 *
 * @author alexclin at 16/1/29 21:15
 */
public abstract class Call {

    public final <T> Handle async(Callback<T> callback){
        return async(Util.type(Callback.class,callback)!=Response.class,callback);
    }

    public abstract <T> Handle async(boolean callOnMain,Callback<T> callback);

    public final Response sync() throws Exception{
        return sync(new Clazz<Response>() {});
    }

    public abstract <T> T sync(Clazz<T> clazz) throws Exception;

    public abstract <T> Result<T> syncResult(Clazz<T> clazz);

    interface CallFactory {
        Call newCall(Request request);
    }

    public abstract Request request();
}
