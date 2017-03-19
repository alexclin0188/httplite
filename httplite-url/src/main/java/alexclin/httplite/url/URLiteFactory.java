package alexclin.httplite.url;

import android.text.TextUtils;
import android.util.Pair;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import alexclin.httplite.LiteClient;
import alexclin.httplite.RequestBody;
import alexclin.httplite.impl.ProgressRunnable;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.ProgressListener;

/**
 * @author xiehonglin429 on 2017/3/19.
 */

public class URLiteFactory implements LiteClient.Converter<RequestBody> {
    @Override
    public RequestBody createRequestBody(RequestBody requestBody, String mediaType) {
        return convertBody(requestBody,mediaType,null);
    }

    @Override
    public RequestBody createRequestBody(File file, String mediaType) {
        return URLRequestBody.create(URLMediaType.parse(mediaType), file);
    }

    @Override
    public RequestBody createRequestBody(String content, String mediaType) {
        return URLRequestBody.create(URLMediaType.parse(mediaType), content);
    }

    @Override
    public RequestBody createRequestBody(byte[] content, String mediaType, int offset, int byteCount) {
        return URLRequestBody.create(URLMediaType.parse(mediaType), content, offset, byteCount);
    }

    @Override
    public RequestBody createRequestBody(List<Pair<String, String>> paramList, List<Pair<String, String>> encodedParamList) {
        URLFormBody.Builder builder = new URLFormBody.Builder();
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
    public RequestBody createRequestBody(String boundary, String type, List<RequestBody> bodyList, List<Pair<Map<String, List<String>>, RequestBody>> headBodyList, List<Pair<String, String>> paramList, List<Pair<String, Pair<String, RequestBody>>> fileList) {
        URLMultipartBody.Builder builder;
        MediaType mediaType = URLMediaType.parse(type);
        if (boundary == null) {
            builder = new URLMultipartBody.Builder().setType(mediaType);
        } else {
            builder = new URLMultipartBody.Builder(boundary).setType(mediaType);
        }
        if (bodyList != null) {
            for (RequestBody body : bodyList) {
                builder.addPart(body);
            }
        }
        if (headBodyList != null) {
            for (Pair<Map<String, List<String>>, RequestBody> bodyPair : headBodyList) {
                builder.addPart(bodyPair.first, bodyPair.second);
            }
        }
        if (paramList != null) {
            for (Pair<String, String> pair : paramList) {
                builder.addFormDataPart(pair.first, pair.second);
            }
        }
        if (fileList != null) {
            for (Pair<String, Pair<String, RequestBody>> pair : fileList) {
                builder.addFormDataPart(pair.first, pair.second.first, pair.second.second);
            }
        }
        return builder.build();
    }

    private RequestBody convertBody(alexclin.httplite.RequestBody requestBody,String mediaType,ProgressListener listener) {
        if (requestBody == null) return null;
        if(requestBody instanceof ProgressBody){
            if(!TextUtils.isEmpty(mediaType))
                ((ProgressBody) requestBody).mediaType = mediaType;
            if(listener!=null)
                ((ProgressBody) requestBody).listener = listener;
            return requestBody;
        }else if (requestBody instanceof alexclin.httplite.RequestBody.NotBody) {
            requestBody = ((alexclin.httplite.RequestBody.NotBody) requestBody).createReal(this);
        }
        if(TextUtils.isEmpty(mediaType)&&listener==null)
            return requestBody;
        return new ProgressBody(requestBody,mediaType,listener);
    }

    private static class ProgressBody extends RequestBody{
        private String mediaType;
        private ProgressListener listener;
        private RequestBody requestBody;

        private long contentLength;

        private ProgressBody(RequestBody requestBody,String mediaType, ProgressListener listener) {
            this.mediaType = mediaType;
            this.listener = listener;
            this.requestBody = requestBody;
        }

        @Override
        public String contentType() {
            if(!TextUtils.isEmpty(mediaType)){
                return mediaType;
            }
            return requestBody.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            contentLength = requestBody.contentLength();
            return contentLength;
        }

        @Override
        public void writeTo(OutputStream out) throws IOException {
            if(listener==null){
                requestBody.writeTo(out);
            }else{
                ProgressOutputStream progressOs = new ProgressOutputStream(out,listener,contentLength);
                progressOs.startProgress();
                try {
                    requestBody.writeTo(progressOs);
                } finally {
                    progressOs.stopProgress();
                }
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
}
