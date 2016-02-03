package alexclin.httplite.urlconnection;

import android.net.Uri;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.util.IOUtil;
import alexclin.httplite.util.Util;

/**
 * alexclin.httplite.urlconnection
 *
 * @author alexclin
 * @date 16/1/2 20:01
 */
public class URLFormBody implements RequestBody {
    private static final URLMediaType CONTENT_TYPE =
            URLMediaType.parse("application/x-www-form-urlencoded");

    private final List<String> encodedNames;
    private final List<String> encodedValues;

    private URLFormBody(List<String> encodedNames, List<String> encodedValues) {
        this.encodedNames = Util.immutableList(encodedNames);
        this.encodedValues = Util.immutableList(encodedValues);
    }

    /** The number of key-name pairs in this form-encoded body. */
    public int size() {
        return encodedNames.size();
    }

    public String encodedName(int index) {
        return encodedNames.get(index);
    }

    public String name(int index) {
        return Uri.decode(encodedName(index));
    }

    public String encodedValue(int index) {
        return encodedValues.get(index);
    }

    public String value(int index) {
        return Uri.decode(encodedValue(index));
    }

    @Override public MediaType contentType() {
        return CONTENT_TYPE;
    }

    @Override public long contentLength() {
        return writeOrCountBytes(null, true);
    }

    /**
     * Either writes this call to {@code sink} or measures its content length. We have one method
     * do double-duty to make sure the counting and content are consistent, particularly when it comes
     * to awkward operations like measuring the encoded length of header strings, or the
     * length-in-digits of an encoded integer.
     */
    private long writeOrCountBytes(OutputStream sink, boolean countBytes) {
        long byteCount = 0L;

        BufferedWriter buffer;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        if (countBytes) {
            buffer = new BufferedWriter(new OutputStreamWriter(new ByteArrayOutputStream(), Util.UTF_8));
        } else {
            buffer = new BufferedWriter(new OutputStreamWriter(sink,Util.UTF_8));
        }

        try {
            for (int i = 0, size = encodedNames.size(); i < size; i++) {
                if (i > 0) buffer.write('&');
                buffer.write(encodedNames.get(i));
                buffer.write('=');
                buffer.write(encodedValues.get(i));
            }

            if (countBytes) {
                byteCount = bos.size();
                IOUtil.closeQuietly(buffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteCount;
    }

    public static final class Builder {
        private final List<String> names = new ArrayList<>();
        private final List<String> values = new ArrayList<>();

        public Builder add(String name, String value) {
            names.add(Uri.encode(name, Util.UTF_8.name()));
            values.add(Uri.encode(value, Util.UTF_8.name()));
            return this;
        }

        public Builder addEncoded(String name, String value) {
            names.add(name);
            values.add(value);
            return this;
        }

        public URLFormBody build() {
            return new URLFormBody(names, values);
        }
    }

    @Override
    public void writeTo(OutputStream sink) throws IOException {
        writeOrCountBytes(sink, false);
    }
}
