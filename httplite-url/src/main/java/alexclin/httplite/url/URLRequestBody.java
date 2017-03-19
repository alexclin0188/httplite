package alexclin.httplite.url;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import alexclin.httplite.listener.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.util.Util;

/**
 * URLRequestBody
 *
 * @author alexclin  16/1/2 19:32
 */
public abstract class URLRequestBody extends RequestBody{

    public static RequestBody create(MediaType contentType, String content) {
        Charset charset = Util.UTF_8;
        if (contentType != null) {
            charset = contentType.charset();
            if (charset == null) {
                charset = Util.UTF_8;
                contentType = URLMediaType.parse(contentType + "; charset=utf-8");
            }
        }
        byte[] bytes = content.getBytes(charset);
        return create(contentType, bytes);
    }

    /** Returns a new call body that transmits {@code content}. */
    public static RequestBody create(final MediaType contentType, final byte[] content) {
        return create(contentType, content, 0, content.length);
    }

    /** Returns a new call body that transmits {@code content}. */
    public static RequestBody create(final MediaType contentType, final byte[] content,
                                     final int offset, final int byteCount) {
        if (content == null) throw new NullPointerException("content == null");
        Util.checkOffsetAndCount(content.length, offset, byteCount);
        return new URLRequestBody() {
            @Override public String contentType() {
                return contentType.toString();
            }

            @Override public long contentLength() {
                return byteCount;
            }

            @Override public void writeTo(OutputStream sink) throws IOException {
                sink.write(content, offset, byteCount);
            }
        };
    }

    /** Returns a new call body that transmits the content of {@code file}. */
    public static RequestBody create(final MediaType contentType, final File file) {
        if (file == null) throw new NullPointerException("content == null");

        return new URLRequestBody() {
            @Override public String contentType() {
                return contentType.toString();
            }

            @Override public long contentLength() {
                return file.length();
            }

            @Override public void writeTo(OutputStream sink) throws IOException {
                InputStream source = null;
                try {
                    source = new FileInputStream(file);
                    Util.copy(source,sink);
                } finally {
                    Util.closeQuietly(source);
                }
            }
        };
    }

    protected static class CountOutputStream extends OutputStream{

        private long mSize = 0;

        @Override
        public void write(int oneByte) throws IOException {
            mSize++;
        }

        public long countBytes(){
            return mSize;
        }
    }
}
