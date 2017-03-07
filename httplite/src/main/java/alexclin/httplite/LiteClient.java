package alexclin.httplite;

import android.util.Pair;

import java.io.File;
import java.util.List;
import java.util.Map;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.Response;


/**
 * LiteClient
 *
 * @author alexclin at 15/12/31 17:14
 */
public interface LiteClient {

    Response execute(Request request) throws Throwable;

    void enqueue(Request request, Callback<Response> callback);

    void cancel(Object tag);

    void cancelAll();

    void shutDown();

    MediaType mediaType(String mediaType);

    interface Converter<T>{
        T createRequestBody(RequestBody requestBody,String mediaType);
        T createRequestBody(File file, String mediaType);
        T createRequestBody(String content, String mediaType);
        T createRequestBody(byte[] content, String mediaType,int offset,int byteCount);
        T createRequestBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList);
        T createRequestBody(String boundary, String type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
                                    List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList);
    }
}
