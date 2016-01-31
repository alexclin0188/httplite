package alexclin.httplite;

import java.io.IOException;
import java.io.OutputStream;

/**
 * alexclin.httplite
 *
 * @author alexclin
 * @date 16/1/1 10:11
 */
public interface RequestBody {
    /** Returns the Content-Type header for this body. */
    MediaType contentType();

    /**
     * Returns the number of bytes that will be written to {@code out} in a call to {@link #writeTo},
     * or -1 if that count is unknown.
     */
    long contentLength() throws IOException;

    /** Writes the content of this call to {@code out}. */
    void writeTo(OutputStream sink) throws IOException;
}
