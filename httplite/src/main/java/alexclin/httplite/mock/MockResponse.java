package alexclin.httplite.mock;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import alexclin.httplite.MediaType;
import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.ResponseBody;

/**
 * MockResponse
 *
 * @author alexclin  16/3/10 21:13
 */
public class MockResponse implements Response {
    private Request request;
    private Map<String, List<String>> headers;
    private ResponseBody body;
    private int code;
    private String msg;

    public MockResponse(Request request,int code ,String msg,Map<String, List<String>> headers,InputStream inputStream,MediaType type) {
        this.request = request;
        this.headers = headers;
        this.code = code;
        this.msg = msg;
        this.body = new MockBody(inputStream,type);
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String message() {
        return msg;
    }

    @Override
    public List<String> headers(String name) {
        return headers==null?null:headers.get(name);
    }

    @Override
    public String header(String name) {
        List<String> list = headers(name);
        return (list==null||list.isEmpty())?null:list.get(0);
    }

    @Override
    public Map<String, List<String>> headers() {
        return headers;
    }

    @Override
    public ResponseBody body() {
        return body;
    }
}

class MockBody implements ResponseBody{
    private InputStream inputStream;
    private MediaType mediaType;

    MockBody(InputStream inputStream, MediaType mediaType) {
        this.inputStream = inputStream;
        this.mediaType = mediaType;
    }

    @Override
    public MediaType contentType() {
        return mediaType;
    }

    @Override
    public long contentLength() throws IOException {
        return inputStream.available();
    }

    @Override
    public InputStream stream() throws IOException {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if(inputStream!=null)
            inputStream.close();
    }
}
