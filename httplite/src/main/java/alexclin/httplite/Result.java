package alexclin.httplite;

import java.util.List;
import java.util.Map;

/**
 * Result
 *
 * @author alexclin  16/3/18 22:57
 */
public class Result<T> {
    private final T result;
    private final Map<String,List<String>> headers;
    private final Throwable throwable;

    public Result(Throwable throwable) {
        this(null,null,throwable);
    }

    public Result(T result, Map<String, List<String>> headers) {
        this(result,headers,null);
    }

    public Result(T result, Map<String, List<String>> headers, Throwable throwable) {
        this.result = result;
        this.headers = headers;
        this.throwable = throwable;
    }


    public T result() {
        return result;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public Throwable error() {
        return throwable;
    }
}
