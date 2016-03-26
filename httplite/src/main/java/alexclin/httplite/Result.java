package alexclin.httplite;

import java.util.List;
import java.util.Map;

/**
 * Result
 *
 * @author alexclin  16/3/18 22:57
 */
public class Result<T> {
    private T result;
    private Map<String,List<String>> headers;
    private Throwable throwable;

    Result(T result, Map<String, List<String>> headers) {
        this.result = result;
        this.headers = headers;
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

    public Throwable getError() {
        return throwable;
    }

    public void setError(Throwable throwable) {
        this.throwable = throwable;
    }
}
