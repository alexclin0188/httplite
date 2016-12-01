package alexclin.httplite.listener;

import java.util.List;
import java.util.Map;

import alexclin.httplite.Request;

/**
 * Response
 *
 * @author  alexclin  16/1/1 10:11
 */
public interface Response {
    Request request();
    int code();
    String message();
    List<String> headers(String name);
    String header(String name);
    Map<String,List<String>> headers();
    ResponseBody body();
}
