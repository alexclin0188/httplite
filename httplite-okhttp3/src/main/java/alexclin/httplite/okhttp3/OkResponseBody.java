package alexclin.httplite.okhttp3;

import java.io.IOException;
import java.io.InputStream;

import alexclin.httplite.MediaType;
import okhttp3.ResponseBody;

/**
 * OkResponseBody
 *
 * @author alexclin 16/1/1 15:00
 */
public class OkResponseBody implements alexclin.httplite.ResponseBody {
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
