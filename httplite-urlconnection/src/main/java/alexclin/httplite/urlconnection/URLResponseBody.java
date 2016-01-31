package alexclin.httplite.urlconnection;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import alexclin.httplite.MediaType;
import alexclin.httplite.ResponseBody;
import alexclin.httplite.util.IOUtil;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/2 19:31
 */
public class URLResponseBody implements ResponseBody {

    private MediaType type;
    private long contentLength;
    private InputStream stream;

    public URLResponseBody(HttpURLConnection urlConnection) throws IOException{
        contentLength = urlConnection.getContentLength();
        type = URLMediaType.parse(urlConnection.getContentType());
        try {
            stream = urlConnection.getInputStream();
        } catch (IOException ioe) {
            stream = urlConnection.getErrorStream();
        }
    }

    @Override
    public MediaType contentType() {
        return type;
    }

    @Override
    public long contentLength() throws IOException {
        return contentLength;
    }

    @Override
    public InputStream stream() throws IOException {
        return stream;
    }

    @Override
    public void close() throws IOException {
        IOUtil.closeQuietly(stream);
    }
}
