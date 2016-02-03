package alexclin.httplite.urlconnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.util.IOUtil;
import alexclin.httplite.util.Util;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/2 19:32
 */
public abstract class URLRequestBody implements RequestBody{

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
            @Override public MediaType contentType() {
                return contentType;
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
            @Override public MediaType contentType() {
                return contentType;
            }

            @Override public long contentLength() {
                return file.length();
            }

            @Override public void writeTo(OutputStream sink) throws IOException {
                InputStream source = null;
                try {
                    source = new FileInputStream(file);
                    IOUtil.copy(source,sink);
                } finally {
                    IOUtil.closeQuietly(source);
                }
            }
        };
    }
}
