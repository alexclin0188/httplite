package alexclin.httplite.okhttp2;

import com.squareup.okhttp.RequestBody;

import java.io.IOException;
import java.io.OutputStream;

import alexclin.httplite.MediaType;
import okio.BufferedSink;
import okio.Okio;

/**
 * alexclin.httplite.okhttp
 *
 * @author alexclin
 * @date 16/1/1 14:22
 */
public class RequestBodyWrapper implements alexclin.httplite.RequestBody {
    private RequestBody requestBody;
    private MediaType type;

    public RequestBodyWrapper(RequestBody requestBody) {
        this.requestBody = requestBody;
        this.type = new MediaTypeWrapper(requestBody.contentType());
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
        requestBody.writeTo(Okio.buffer(Okio.sink(os)));
    }

    public RequestBody raw(){
        return requestBody;
    }

    public static RequestBody wrapperLite(final alexclin.httplite.RequestBody requestBody){
        if(requestBody==null) return null;
        if(requestBody instanceof RequestBodyWrapper){
            return ((RequestBodyWrapper) requestBody).raw();
        }else{
            return new RequestBody() {
                @Override
                public com.squareup.okhttp.MediaType contentType() {
                    return MediaTypeWrapper.wrapperLite(requestBody.contentType());
                }

                @Override
                public void writeTo(BufferedSink sink) throws IOException {
                    requestBody.writeTo(sink.outputStream());
                }
            };
        }
    }
}
