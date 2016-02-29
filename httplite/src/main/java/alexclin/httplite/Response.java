package alexclin.httplite;

import java.util.List;
import java.util.Map;

/**
 * Response
 *
 * @author  alexclin
 * @date 16/1/1 10:11
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
