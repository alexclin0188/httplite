package alexclin.httplite.impl;

import java.io.IOException;
import java.io.OutputStream;

import alexclin.httplite.MediaType;
import alexclin.httplite.RequestBody;
import alexclin.httplite.listener.ProgressListener;

/**
 * ProgressRequestBody
 *
 * @author alexclin  16/3/31 22:21
 */
public class ProgressRequestBody implements RequestBody {
    private RequestBody requestBody;
    private ProgressListener progressListener;

    public ProgressRequestBody(RequestBody requestBody, ProgressListener progressListener) {
        this.requestBody = requestBody;
        this.progressListener = progressListener;
    }

    private long contentLength;

    public boolean isWrappBody(RequestBody body){
        return requestBody==body;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        contentLength = requestBody.contentLength();
        return contentLength;
    }

    @Override
    public void writeTo(OutputStream sink) throws IOException {
        if(contentLength>0){
            ProgressOutputStream outputStream = new ProgressOutputStream(sink,progressListener,contentLength);
            outputStream.startProgress();
            try {
                requestBody.writeTo(outputStream);
            } finally {
                outputStream.stopProgress();
            }
        }else{
            requestBody.writeTo(sink);
        }
    }

    private class ProgressOutputStream extends OutputStream implements ProgressRunnable.ProgressSource{
        private OutputStream outputStream;
        private long total = 0;
        private ProgressListener progressListener;
        private ProgressRunnable runnable;

        public ProgressOutputStream(OutputStream outputStream,ProgressListener listener,long total) {
            this.outputStream = outputStream;
            this.progressListener = listener;
            this.runnable = new ProgressRunnable(true,total,this);
        }

        @Override
        public void write(int oneByte) throws IOException {
            outputStream.write(oneByte);
            total++;
        }

        @Override
        public long progress() {
            return total;
        }

        @Override
        public void onProgressUpdate(boolean out, long current, long total) {
            progressListener.onProgressUpdate(out,current,total);
        }

        public void startProgress(){
            runnable.run();
        }

        public void stopProgress(){
            runnable.end();
            ProgressRequestBody.this.contentLength = 0;
        }
    }
}
