package alexclin.httplite;

import android.util.Pair;

import java.io.File;
import java.net.CookieHandler;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import java.util.Map;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;


/**
 * LiteClient
 *
 * @author alexclin at 15/12/31 17:14
 */
public interface LiteClient {

    Handle execute(Request request, ResultCallback callback,Runnable preWork);

    Response executeSync(Request request) throws Exception;

    void cancel(Object tag);

    RequestBody createRequestBody(MediaType contentType, String content);

    RequestBody createRequestBody(final MediaType contentType, final byte[] content);

    RequestBody createRequestBody(final MediaType contentType, final byte[] content,
                                  final int offset, final int byteCount);

    RequestBody createRequestBody(final MediaType contentType, final File file);

    MediaType parse(String type);

    RequestBody createMultipartBody(String boundary, MediaType type, List<RequestBody> bodyList, List<Pair<Map<String,List<String>>,RequestBody>> headBodyList,
                                    List<Pair<String,String>> paramList, List<Pair<String,Pair<String,RequestBody>>> fileList);

    RequestBody createFormBody(List<Pair<String,String>> paramList, List<Pair<String,String>> encodedParamList);

    void setConfig(ClientSettings settings);
}
