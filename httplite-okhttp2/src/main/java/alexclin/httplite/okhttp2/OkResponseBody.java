package alexclin.httplite.okhttp2;

import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

import alexclin.httplite.listener.MediaType;

/**
 * OkResponseBody
 *
 * @author alexclin 16/1/1 15:00
 */
public class OkResponseBody implements alexclin.httplite.listener.ResponseBody {
    private ResponseBody realBody;
    private MediaType type;
    public OkResponseBody(ResponseBody realBody) {
        this.realBody = realBody;
        this.type = new OkMediaType(realBody.contentType());
    }

    @Override
    public MediaType contentType() {
        return type;
    }

    @Override
    public long contentLength() throws IOException{
        return realBody.contentLength();
    }

    @Override
    public InputStream stream() throws IOException{
        return realBody.byteStream();
    }

    @Override
    public void close() throws IOException {
        realBody.close();
    }
}
