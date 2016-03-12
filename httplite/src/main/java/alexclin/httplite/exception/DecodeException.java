package alexclin.httplite.exception;

/**
 * DecodeException
 *
 * @author alexclin 16/1/1 18:55
 */
public class DecodeException extends Exception {

    public DecodeException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }
}
