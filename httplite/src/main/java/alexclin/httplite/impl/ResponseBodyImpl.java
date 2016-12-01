package alexclin.httplite.impl;

import java.io.IOException;
import java.io.InputStream;

import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.ResponseBody;

/**
 * ResponseBodyImpl
 *
 * @author alexclin  16/3/12 14:42
 */
public class ResponseBodyImpl implements ResponseBody {
    private InputStream inputStream;
    private MediaType mediaType;
    private long contentLength;

    public ResponseBodyImpl(InputStream inputStream, MediaType mediaType,long contentLength) {
        this.inputStream = inputStream;
        this.mediaType = mediaType;
        this.contentLength = contentLength;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return contentLength;
    }

    @Override
    public InputStream stream() throws IOException {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if(inputStream!=null)
            inputStream.close();
    }
}
