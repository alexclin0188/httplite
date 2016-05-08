package alexclin.httplite.okhttp3;

import java.io.IOException;
import java.io.OutputStream;

import alexclin.httplite.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;

/**
 * OkRequestBody
 *
 * @author alexclin 16/1/1 14:22
 */
public class OkRequestBody implements alexclin.httplite.RequestBody {
    private RequestBody requestBody;
    private MediaType type;

    public OkRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
        this.type = new OkMediaType(requestBody.contentType());
    }

    @Override
    public MediaType contentType() {
        return type;
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        BufferedSink sink = Okio.buffer(Okio.sink(os));
        requestBody.writeTo(sink);
        sink.flush();
    }

    public RequestBody raw(){
        return requestBody;
    }

    public static RequestBody wrapperLite(final alexclin.httplite.RequestBody requestBody){
        if(requestBody==null) return null;
        if(requestBody instanceof OkRequestBody){
            return ((OkRequestBody) requestBody).raw();
        }else{
            return new RequestBody() {
                @Override
                public okhttp3.MediaType contentType() {
                    return OkMediaType.wrapperLite(requestBody.contentType());
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    requestBody.writeTo(sink.outputStream());
                }
            };
        }
    }
}
