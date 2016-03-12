package alexclin.httplite.exception;

/**
 * ParserException
 *
 * @author alexclin 16/1/1 10:15
 */
public class ParserException extends RuntimeException{
    public ParserException(String detailMessage) {
        super(detailMessage);
    }

    public ParserException(Throwable throwable) {
        super(throwable);
    }
}
