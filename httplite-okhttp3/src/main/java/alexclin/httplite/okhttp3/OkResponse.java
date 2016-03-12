package alexclin.httplite.okhttp3;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.ResponseBody;
import okhttp3.Response;

/**
 * OkResponse
 *
 * @author alexclin 16/1/1 15:02
 */
public class OkResponse implements alexclin.httplite.Response {
    private Response realResponse;
    private Request request;

    public OkResponse(Response realResponse, Request request) {
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
}
