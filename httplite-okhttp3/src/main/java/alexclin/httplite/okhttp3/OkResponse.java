package alexclin.httplite.okhttp3;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.listener.MediaType;
import alexclin.httplite.listener.ResponseBody;
import okhttp3.Response;

/**
 * OkResponse
 *
 * @author alexclin 16/1/1 15:02
 */
class OkResponse implements alexclin.httplite.listener.Response {
    private Response realResponse;
    private Request request;

    OkResponse(Response realResponse, Request request) {
        this.realResponse = realResponse;
        this.request = request;
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public int code() {
        return realResponse.code();
    }

    @Override
    public String message() {
        return realResponse.message();
    }

    @Override
    public List<String> headers(String name) {
        return realResponse.headers(name);
    }

    @Override
    public String header(String name) {
        return realResponse.header(name);
    }

    @Override
    public Map<String, List<String>> headers() {
        return realResponse.headers().toMultimap();
    }

    @Override
    public ResponseBody body() {
        return new OkResponseBody(realResponse.body());
    }

    private static class OkResponseBody implements alexclin.httplite.listener.ResponseBody {
        private okhttp3.ResponseBody realBody;
        private MediaType type;
        OkResponseBody(okhttp3.ResponseBody realBody) {
            this.realBody = realBody;
            this.type = new OkMediaType(realBody.contentType());
        }

        @Override
        public MediaType contentType() {
            return type;
        }

        @Override
        public long contentLength() throws IOException {
            return realBody.contentLength();
        }

        @Override
        public InputStream stream() throws IOException{
            return realBody.byteStream();
        }

        @Override
        public void close() throws IOException {
            realBody.close();
        }
    }
}
