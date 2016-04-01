package alexclin.httplite.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.ResponseBody;
import alexclin.httplite.listener.ProgressListener;

/**
 * ProgressResponse
 *
 * @author alexclin  16/3/31 22:37
 */
public class ProgressResponse implements Response {

    private Response response;
    private ResponseBody body;

    public ProgressResponse(Response response,ProgressListener listener) {
        this.response = response;
        this.body = new ProgressResponseBody(response.body(),listener);
    }

    @Override
    public Request request() {
        return response.request();
    }

    @Override
    public int code() {
        return response.code();
    }

    @Override
    public String message() {
        return response.message();
    }

    @Override
    public List<String> headers(String name) {
        return response.headers(name);
    }

    @Override
    public String header(String name) {
        return response.header(name);
    }

    @Override
    public Map<String, List<String>> headers() {
        return response.headers();
    }

    @Override
    public ResponseBody body() {
        return body;
    }

    private static class ProgressResponseBody implements ResponseBody{
        private ResponseBody responseBody;
        private ProgressListener progressListener;

        private long contentLength;

        public ProgressResponseBody(ResponseBody responseBody, ProgressListener progressListener) {
            this.responseBody = responseBody;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return responseBody.contentType();
        }

        @Override
        public long contentLength() throws IOException {
            contentLength = responseBody.contentLength();
            return contentLength;
        }

        @Override
        public InputStream stream() throws IOException {
            return new ProgressInputStream(responseBody.stream(),progressListener,contentLength);
        }

        @Override
        public void close() throws IOException {
            responseBody.close();
        }
    }

    private static class ProgressInputStream extends InputStream implements ProgressRunnable.ProgressSource{
        private InputStream inputStream;
        private ProgressListener progressListener;
        private volatile long total = 0;
        private ProgressRunnable runnable;


        public ProgressInputStream(InputStream inputStream,ProgressListener listener,long total) {
            this.inputStream = inputStream;
            this.progressListener = listener;
            this.runnable = new ProgressRunnable(false,total,this);
        }

        @Override
        public int read() throws IOException {
            int read = inputStream.read();
            total++;
            if(total==0) runnable.run();
            if(read==-1){
                runnable.end();
            }
            return read;
        }

        @Override
        public long progress() {
            return total;
        }

        @Override
        public void onProgressUpdate(boolean out, long current, long total) {
            progressListener.onProgressUpdate(out,current,total);
        }
    }
}