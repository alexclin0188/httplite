package alexclin.httplite;

import java.io.File;

import alexclin.httplite.listener.Callback;

/**
 * Call
 *
 * @author alexclin at 16/1/29 21:15
 */
public interface Call {
    <T> Handle execute(Callback<T> callback);
    Response executeSync() throws Exception;
    <T> T executeSync(Clazz<T> clazz) throws Exception;
    DownloadHandle download(Callback<File> callback);
    interface CallFactory {
        Call newCall(Request request);
    }
}
