package alexclin.httplite;

import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import alexclin.httplite.listener.MediaType;

/**
 * RequestBody
 *
 * @author alexclin 16/1/1 10:11
 */
public abstract class RequestBody {
    /**
     * Returns the Content-Type header for this body.
     * @return Content-Type
     */
    public abstract String contentType();

    /**
     * Returns the number of bytes that will be written to {@code out} in a call to {@link #writeTo},
     * or -1 if that count is unknown.
     * @return contentLength
     * @throws IOException Exception
     */
    public abstract long contentLength() throws IOException;

    /**
     * Writes the content of this call to {@code out}.
     * @param sink ouputStream
     * @throws IOException Exception in wtrite stream
     */
    public abstract void writeTo(OutputStream sink) throws IOException;

    public static abstract class NotBody extends RequestBody {
        public String mediaType;

        @Override
        public String contentType() {
            throw new IllegalStateException("This method should never be executed");
        }

        @Override
        public long contentLength() throws IOException {
            throw new IllegalStateException("This method should never be executed");
        }

        @Override
        public void writeTo(OutputStream sink) throws IOException {
            throw new IllegalStateException("This method should never be executed");
        }

        public void setMediaType(String mediaType) {
            this.mediaType = mediaType;
        }

        public abstract <T> T createReal(LiteClient.Converter<T> factory);
    }

    static RequestBody wrapBody(RequestBody body, String mediaType) {
        return new WrapBody(body, mediaType);
    }

    public static RequestBody createBody(File file, String mediaType) {
        return new FileBody(file, mediaType);
    }

    public static RequestBody createBody(String content, String mediaType) {
        return new StringBody(content, mediaType);
    }

    public static RequestBody createBody(byte[] content, String mediaType) {
        return new BytesBody(content, mediaType,0,content==null?0:content.length);
    }

    public static RequestBody createBody(byte[] content, String mediaType,int offset,int byteCount){
        return new BytesBody(content,mediaType,offset,byteCount);
    }

    private static final class FileBody extends NotBody {
        private File file;

        private FileBody(File file, String mediaType) {
            this.mediaType = mediaType;
            this.file = file;
        }

        @Override
        public <T> T createReal(LiteClient.Converter<T> factory) {
            return factory.createRequestBody(file, mediaType);
        }
    }

    private static final class StringBody extends NotBody {
        private String content;

        private StringBody(String content, String mediaType) {
            this.content = content;
            this.mediaType = mediaType;
        }

        @Override
        public <T> T createReal(LiteClient.Converter<T> factory) {
            return factory.createRequestBody(content, mediaType);
        }
    }

    private static final class BytesBody extends NotBody {
        private byte[] content;
        private int offset;
        private int byteCount;

        BytesBody(byte[] content, String mediaType,int byteCount, int offset) {
            this.mediaType = mediaType;
            this.byteCount = byteCount;
            this.content = content;
            this.offset = offset;
        }

        @Override
        public <T> T createReal(LiteClient.Converter<T> factory) {
            return factory.createRequestBody(content, mediaType,offset,byteCount);
        }
    }

    private static final class WrapBody extends NotBody {
        private RequestBody realBody;

        WrapBody(RequestBody realBody, String mediaType) {
            this.realBody = realBody;
            this.mediaType = mediaType;
        }

        @Override
        public <T> T createReal(LiteClient.Converter<T> factory) {
            return factory.createRequestBody(realBody, mediaType);
        }
    }

    static final class FormBody extends NotBody {
        private List<Pair<String, String>> paramList;
        private List<Pair<String, String>> encodedParamList;

        FormBody() {
        }

        public FormBody add(String name, String value) {
            if (paramList == null) {
                paramList = new ArrayList<>();
            }
            paramList.add(new Pair<String, String>(name, value));
            return this;
        }

        public FormBody addEncoded(String name, String value) {
            if (encodedParamList == null) {
                encodedParamList = new ArrayList<>();
            }
            encodedParamList.add(new Pair<String, String>(name, value));
            return this;
        }

        @Override
        public <T> T createReal(LiteClient.Converter<T> factory) {
            return factory.createRequestBody(paramList, encodedParamList);
        }
    }

    static final class MultipartBody extends NotBody {
        private String boundary;
        private String type;
        private List<RequestBody> bodyList;
        private List<Pair<Map<String, List<String>>, RequestBody>> headBodyList;
        private List<Pair<String, String>> paramList;
        private List<Pair<String, Pair<String, RequestBody>>> fileList;

        MultipartBody() {
        }

        public MultipartBody boundary(String boundary) {
            this.boundary = boundary;
            return this;
        }

        public MultipartBody setType(String type) {
            this.type = type;
            return this;
        }

        public MultipartBody add(RequestBody body) {
            if (bodyList == null) {
                bodyList = new ArrayList<>();
            }
            this.bodyList.add(body);
            return this;
        }

        public MultipartBody add(Map<String, List<String>> headers, RequestBody body) {
            if (headBodyList == null) {
                headBodyList = new ArrayList<>();
            }
            headBodyList.add(new Pair<Map<String, List<String>>, RequestBody>(headers, body));
            return this;
        }

        public MultipartBody add(String name, String value) {
            if (paramList == null) {
                paramList = new ArrayList<>();
            }
            paramList.add(new Pair<String, String>(name, value));
            return this;
        }

        public MultipartBody add(String name, String fileName, RequestBody body) {
            if (fileList == null) {
                fileList = new ArrayList<>();
            }
            fileList.add(new Pair<String, Pair<String, RequestBody>>(name, new Pair<String, RequestBody>(fileName, body)));
            return this;
        }

        @Override
        public <T> T createReal(LiteClient.Converter<T> factory) {
            return factory.createRequestBody(boundary, type, bodyList, headBodyList, paramList, fileList);
        }
    }
}
