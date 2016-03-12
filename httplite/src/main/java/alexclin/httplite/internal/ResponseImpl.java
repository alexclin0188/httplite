package alexclin.httplite.internal;


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


