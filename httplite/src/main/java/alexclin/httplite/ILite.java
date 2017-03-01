package alexclin.httplite;

import android.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.Response;
import alexclin.httplite.util.ClientSettings;


/**
 * ILite
 *
 * @author alexclin at 15/12/31 17:14
 */
public interface ILite {

    Response execute(Request request) throws Throwable;

    void enqueue(Request request, Callback<Response> callback);

    void cancel(Object tag);

    void cancelAll();

    void setConfig(ClientSettings settings);

    void shutDown();

    //    RequestBody createRequestBody(MediaType contentType, String content);
//
//    RequestBody createRequestBody(final MediaType contentType, final byte[] content);
//
//    RequestBody createRequestBody(final MediaType contentType, final byte[] content,
//                                  final int offset, final int byteCount);
//
//    RequestBody createRequestBody(final MediaType contentType, final File file);
//
//    RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
//                                    List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList);
//
//    RequestBody createFormBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList);

    interface RequestBodyFactory<T>{
        T createRequestBody(RequestBody requestBody,String mediaType);
        T createRequestBody(File file, String mediaType);
        T createRequestBody(String content, String mediaType);
        T createRequestBody(byte[] content, String mediaType,int offset,int byteCount);
        T createRequestBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList);
        T createRequestBody(String boundary, String type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
                                    List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList);
    }
}
