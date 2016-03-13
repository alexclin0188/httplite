package alexclin.httplite.internal;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;
import alexclin.httplite.Response;
import alexclin.httplite.ResponseBody;

/**
 * ResponseImpl
 *
 * @author alexclin  16/3/10 21:13
 */
public class ResponseImpl implements Response {
    private Request request;
    private Map<String, List<String>> headers;
    private ResponseBody body;
    private int code;
    private String msg;

    public ResponseImpl(Request request, int code, String msg, Map<String, List<String>> headers, ResponseBody body) {
        this.request = request;
        this.headers = headers;
        this.code = code;
        this.msg = msg;
        this.body = body;
    }

    public ResponseImpl(Response response,InputStream inputStream,long length){
        this.request = response.request();
        this.headers = response.headers();
        this.code = response.code();
        this.msg = response.message();
        this.body = new ResponseBodyImpl(inputStream,response.body().contentType(),length);
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


