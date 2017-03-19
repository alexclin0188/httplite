package alexclin.httplite.okhttp2;

import android.text.TextUtils;
import android.util.Pair;

import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.MultipartBuilder;
import com.squareup.okhttp.RequestBody;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import alexclin.httplite.LiteClient;
import alexclin.httplite.impl.ProgressRunnable;
import alexclin.httplite.listener.ProgressListener;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * @author xiehonglin429 on 2017/3/16.
 */

class Ok2Factory implements LiteClient.Converter<RequestBody> {
    @Override
    public RequestBody createRequestBody(alexclin.httplite.RequestBody requestBody, String mediaType) {
        return convertBody(requestBody,mediaType,null);
    }

    @Override
    public RequestBody createRequestBody(File file, String mediaType) {
        return RequestBody.create(MediaType.parse(mediaType), file);
    }

    @Override
    public RequestBody createRequestBody(String content, String mediaType) {
        return RequestBody.create(MediaType.parse(mediaType), content);
    }

    @Override
    public RequestBody createRequestBody(byte[] content, String mediaType, int offset, int byteCount) {
        return RequestBody.create(MediaType.parse(mediaType), content, offset, byteCount);
    }

    @Override
    public RequestBody createRequestBody(List<Pair<String, String>> paramList, List<Pair<String, String>> encodedParamList) {
        FormEncodingBuilder builder = new FormEncodingBuilder();
        if (paramList != null) {
            for (Pair<String, String> param : paramList) {
                builder.add(param.first, param.second);
            }
        }
        if (encodedParamList != null) {
            for (Pair<String, String> param : encodedParamList) {
                builder.addEncoded(param.first, param.second);
            }
        }
        return builder.build();
    }

    @Override
    public RequestBody createRequestBody(String boundary, String type, List<alexclin.httplite.RequestBody> bodyList, List<Pair<Map<String, List<String>>, alexclin.httplite.RequestBody>> headBodyList, List<Pair<String, String>> paramList, List<Pair<String, Pair<String, alexclin.httplite.RequestBody>>> fileList) {
        MultipartBuilder builder;
        if (boundary == null) {
            builder = new MultipartBuilder().type(MediaType.parse(type));
        } else {
            builder = new MultipartBuilder(boundary).type(MediaType.parse(type));
        }
        if (bodyList != null) {
            for (alexclin.httplite.RequestBody body : bodyList) {
                builder.addPart(convertBody(body));
            }
        }
        if (headBodyList != null) {
            for (Pair<Map<String, List<String>>, alexclin.httplite.RequestBody> bodyPair : headBodyList) {
                builder.addPart(Ok2Lite.createHeader(bodyPair.first), convertBody(bodyPair.second));
            }
        }
        if (paramList != null) {
            for (Pair<String, String> pair : paramList) {
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if (fileList != null) {
            for (Pair<String, Pair<String, alexclin.httplite.RequestBody>> pair : fileList) {
                builder.addFormDataPart(pair.first, pair.second.first, convertBody(pair.second.second));
            }
        }
        return builder.build();
    }

    RequestBody convertBody(alexclin.httplite.RequestBody requestBody) {
        return convertBody(requestBody, null, null);
    }

    RequestBody convertBody(alexclin.httplite.RequestBody requestBody, ProgressListener listener) {
        return convertBody(requestBody, null, listener);
    }

    private RequestBody convertBody(alexclin.httplite.RequestBody requestBody, String mediaType, ProgressListener listener) {
        if (requestBody == null) return null;
        if (requestBody instanceof alexclin.httplite.RequestBody.NotBody) {
            RequestBody body = ((alexclin.httplite.RequestBody.NotBody) requestBody).createReal(this);
            return new ProgressRequestBody(body, mediaType, listener);
        } else {
            return new Ok2RequestBody(requestBody, mediaType, listener);
        }
    }

    private static class Ok2RequestBody extends RequestBody {
        private MediaType mediaType;
        private alexclin.httplite.RequestBody requestBody;
        private ProgressListener listener;

        private long contentLength;

        Ok2RequestBody(alexclin.httplite.RequestBody requestBody, String mediaType, ProgressListener listener) {
            this.requestBody = requestBody;
            if (TextUtils.isEmpty(mediaType)) {
                mediaType = requestBody.contentType();
            }
            this.mediaType = MediaType.parse(mediaType);
            this.listener = listener;
        }

        @Override
        public long contentLength() throws IOException {
            contentLength = requestBody.contentLength();
            return contentLength;
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            if (listener != null) {
                ProgressOutputStream progressOs = new ProgressOutputStream(sink.outputStream(), listener, contentLength);
                progressOs.startProgress();
                try {
                    requestBody.writeTo(progressOs);
                } finally {
                    progressOs.stopProgress();
                }
            } else {
                requestBody.writeTo(sink.outputStream());
            }
        }
    }

    private static class ProgressOutputStream extends OutputStream implements ProgressRunnable.ProgressSource {
        private OutputStream outputStream;
        private long value = 0;
        private ProgressListener progressListener;
        private ProgressRunnable runnable;

        ProgressOutputStream(OutputStream outputStream, ProgressListener listener, long total) {
            this.outputStream = outputStream;
            this.progressListener = listener;
            this.runnable = new ProgressRunnable(true, total, this);
        }

        @Override
        public void write(int oneByte) throws IOException {
            value++;
            outputStream.write(oneByte);
        }

        @Override
        public long progress() {
            return value;
        }

        @Override
        public void onProgress(boolean out, long current, long total) {
            progressListener.onProgress(out, current, total);
        }

        public void startProgress() {
            runnable.run();
        }

        public void stopProgress() {
            runnable.end();
        }
    }

    private static class ProgressRequestBody extends RequestBody {
        private RequestBody real;
        private MediaType mediaType;
        private ProgressListener progressListener;

        ProgressRequestBody(RequestBody real, String mediaType, ProgressListener progressListener) {
            this.real = real;
            this.progressListener = progressListener;
            if (TextUtils.isEmpty(mediaType)) {
                this.mediaType = real.contentType();
            } else {
                this.mediaType = MediaType.parse(mediaType);
            }
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            try {
                sink = Okio.buffer(new ProgressSink(sink, progressListener, contentLength()));
                real.writeTo(sink);
                sink.flush();
            } catch (Exception e) {
                throw (e instanceof IOException) ? ((IOException) e) : new IOException(e);
            }
        }
    }

    private static class ProgressSink extends ForwardingSink {
        private ProgressListener listener;
        //当前写入字节数
        private long bytesWritten = 0L;
        //总字节长度，避免多次调用contentLength()方法
        private long contentLength = 0L;

        ProgressSink(Sink realSink, ProgressListener listener, long contentLength) {
            super(realSink);
            this.listener = listener;
            this.contentLength = contentLength;
        }

        @Override
        public void write(Buffer buffer, long byteCount) throws IOException {
            super.write(buffer, byteCount);
            //增加当前写入的字节数
            bytesWritten += byteCount;
            //回调
            if (null != listener) {
                listener.onProgress(true, bytesWritten, contentLength);
            }
        }
    }
}
