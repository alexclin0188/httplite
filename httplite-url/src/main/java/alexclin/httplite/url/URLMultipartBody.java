package alexclin.httplite.url;

import android.net.Uri;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.util.Util;

/**
 * URLMultipartBody
 *
 * @author alexclin 16/1/2 20:00
 */
public class URLMultipartBody implements RequestBody {

    @Override
    public void writeTo(OutputStream sink) throws IOException {
        writeOrCountBytes(sink, false);
    }

    /**
     * The media-type multipart/form-data follows the rules of all multipart MIME data streams as
     * outlined in RFC 2046. In forms, there are a series of fields to be supplied by the user who
     * fills out the form. Each field has a name. Within a given form, the names are unique.
     */
    public static final MediaType FORM = URLMediaType.parse("multipart/form-data");

    private static final byte[] COLONSPACE = {':', ' '};
    private static final byte[] CRLF = {'\r', '\n'};
    private static final byte[] DASHDASH = {'-', '-'};

    private final String boundary;
    private final MediaType originalType;
    private final MediaType contentType;
    private final List<Part> parts;
    private long contentLength = -1L;

    URLMultipartBody(String boundary, MediaType type, List<Part> parts) {
        this.boundary = boundary;
        this.originalType = type;
        this.contentType = URLMediaType.parse(type + "; boundary=" + Uri.encode(boundary,Util.UTF_8.name()));
        this.parts = Util.immutableList(parts);
    }

    public MediaType type() {
        return originalType;
    }

    public String boundary() {
        return boundary;
    }

    /** The number of parts in this multipart body. */
    public int size() {
        return parts.size();
    }

    public List<Part> parts() {
        return parts;
    }

    public Part part(int index) {
        return parts.get(index);
    }

    /** A combination of {@link #type()} and {@link #boundary()}. */
    @Override public MediaType contentType() {
        return contentType;
    }

    @Override public long contentLength() throws IOException {
        long result = contentLength;
        if (result != -1L) return result;
        return contentLength = writeOrCountBytes(null, true);
    }

    /**
     * Either writes this call to {@code sink} or measures its content length. We have one method
     * do double-duty to make sure the counting and content are consistent, particularly when it comes
     * to awkward operations like measuring the encoded length of header strings, or the
     * length-in-digits of an encoded integer.
     */
    private long writeOrCountBytes(OutputStream sink, boolean countBytes) throws IOException {
        long byteCount = 0L;

        try {
            BufferedWriter byteCountBuffer = null;
            BufferedWriter writer;
            ByteArrayOutputStream bos = null;
            if (countBytes) {
                bos = new ByteArrayOutputStream();
                writer = byteCountBuffer = new BufferedWriter(new OutputStreamWriter(bos,Util.UTF_8));
                sink = bos;
            }else{
                writer = new BufferedWriter(new OutputStreamWriter(sink,Util.UTF_8));
            }

            for (int p = 0, partCount = parts.size(); p < partCount; p++) {
                Part part = parts.get(p);
                Map<String, List<String>> headers = part.headers;
                RequestBody body = part.body;

                sink.write(DASHDASH);
                sink.write(boundary.getBytes(Util.UTF_8));
                sink.write(CRLF);

                if (headers != null) {
                    for (String key:headers.keySet()) {
                        for (String value:headers.get(key)){
                            writer.write(key);
                            writer.flush();
                            if(countBytes){
                                bos.write(COLONSPACE);
                            }else{
                                sink.write(COLONSPACE);
                            }
                            writer.write(value);
                            writer.flush();
                            if(countBytes){
                                bos.write(CRLF);
                            }else{
                                sink.write(CRLF);
                            }
                        }
                    }
                }

                MediaType contentType = body.contentType();
                if (contentType != null) {
                    writer.write("Content-Type: ");
                    writer.write(contentType.toString());
                    writer.flush();
                    if(countBytes){
                        bos.write(CRLF);
                    }else{
                        sink.write(CRLF);
                    }
                }

                long contentLength = body.contentLength();
                if (contentLength != -1) {
                    writer.write("Content-Length: ");
                    writer.flush();
                    if(countBytes){
                        bos.write(getBytes(contentLength));
                        bos.write(CRLF);
                    }else{
                        sink.write(getBytes(contentLength));
                        sink.write(CRLF);
                    }
                } else if (countBytes) {
                    // We can't measure the body's size without the sizes of its components.
                    Util.closeQuietly(writer);
                    return -1L;
                }

                sink.write(CRLF);

                if (countBytes) {
                    byteCount += contentLength;
                } else {
                    body.writeTo(sink);
                }

                sink.write(CRLF);
            }

            sink.write(DASHDASH);
            sink.write(boundary.getBytes(Util.UTF_8));
            sink.write(DASHDASH);
            sink.write(CRLF);

            sink.flush();
            if (countBytes) {
                byteCount += bos.size();
                Util.closeQuietly(byteCountBuffer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return byteCount;
    }

    static byte[] getBytes(long data){
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (data & 0xff);
        bytes[1] = (byte) ((data >> 8) & 0xff);
        bytes[2] = (byte) ((data >> 16) & 0xff);
        bytes[3] = (byte) ((data >> 24) & 0xff);
        bytes[4] = (byte) ((data >> 32) & 0xff);
        bytes[5] = (byte) ((data >> 40) & 0xff);
        bytes[6] = (byte) ((data >> 48) & 0xff);
        bytes[7] = (byte) ((data >> 56) & 0xff);
        return bytes;
    }

    /**
     * Appends a quoted-string to a StringBuilder.
     *
     * <p>RFC 2388 is rather vague about how one should escape special characters in form-data
     * parameters, and as it turns out Firefox and Chrome actually do rather different things, and
     * both say in their comments that they're not really sure what the right approach is. We go with
     * Chrome's behavior (which also experimentally seems to match what IE does), but if you actually
     * want to have a good chance of things working, please avoid double-quotes, newlines, percent
     * signs, and the like in your field names.
     */
    static StringBuilder appendQuotedString(StringBuilder target, String key) {
        target.append('"');
        for (int i = 0, len = key.length(); i < len; i++) {
            char ch = key.charAt(i);
            switch (ch) {
                case '\n':
                    target.append("%0A");
                    break;
                case '\r':
                    target.append("%0D");
                    break;
                case '"':
                    target.append("%22");
                    break;
                default:
                    target.append(ch);
                    break;
            }
        }
        target.append('"');
        return target;
    }

    public static final class Part {
        public static Part create(RequestBody body) {
            return create(null, body);
        }

        public static Part create(Map<String, List<String>> headers, RequestBody body) {
            if (body == null) {
                throw new NullPointerException("body == null");
            }
            if (headers != null && headers.get("Content-Type") != null) {
                throw new IllegalArgumentException("Unexpected header: Content-Type");
            }
            if (headers != null && headers.get("Content-Length") != null) {
                throw new IllegalArgumentException("Unexpected header: Content-Length");
            }
            return new Part(headers, body);
        }

        public static Part createFormData(String name, String value) {
            return createFormData(name, null, URLRequestBody.create(null, value));
        }

        public static Part createFormData(String name, String filename, RequestBody body) {
            if (name == null) {
                throw new NullPointerException("name == null");
            }
            StringBuilder disposition = new StringBuilder("form-data; name=");
            appendQuotedString(disposition, name);

            if (filename != null) {
                disposition.append("; filename=");
                appendQuotedString(disposition, filename);
            }
            Map<String, List<String>> headers = new HashMap<>();
            headers.put("Content-Disposition", Collections.singletonList(disposition.toString()));
            return create(headers, body);
        }

        private final Map<String, List<String>> headers;
        private final RequestBody body;

        private Part(Map<String, List<String>> headers, RequestBody body) {
            this.headers = headers;
            this.body = body;
        }
    }

    public static final class Builder {
        private final String boundary;
        private MediaType type = URLMediaType.parse(MediaType.MULTIPART_MIXED);
        private final List<Part> parts = new ArrayList<>();

        public Builder() {
            this(UUID.randomUUID().toString());
        }

        public Builder(String boundary) {
            this.boundary = boundary;
        }

        public Builder setType(MediaType type) {
            if (type == null) {
                throw new NullPointerException("type == null");
            }
            if (!type.type().equals("multipart")) {
                throw new IllegalArgumentException("multipart != " + type);
            }
            this.type = type;
            return this;
        }

        /** Add a part to the body. */
        public Builder addPart(RequestBody body) {
            return addPart(Part.create(body));
        }

        /** Add a part to the body. */
        public Builder addPart(Map<String, List<String>> headers, RequestBody body) {
            return addPart(Part.create(headers, body));
        }

        /** Add a form data part to the body. */
        public Builder addFormDataPart(String name, String value) {
            return addPart(Part.createFormData(name, value));
        }

        /** Add a form data part to the body. */
        public Builder addFormDataPart(String name, String filename, RequestBody body) {
            return addPart(Part.createFormData(name, filename, body));
        }

        /** Add a part to the body. */
        public Builder addPart(Part part) {
            if (part == null) throw new NullPointerException("part == null");
            parts.add(part);
            return this;
        }

        /** Assemble the specified parts into a call body. */
        public URLMultipartBody build() {
            if (parts.isEmpty()) {
                throw new IllegalStateException("Multipart body must have at least one part.");
            }
            return new URLMultipartBody(boundary, type, parts);
        }
    }
}
