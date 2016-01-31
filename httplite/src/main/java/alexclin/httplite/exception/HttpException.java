package alexclin.httplite.exception;

/**
 * alexclin.httplite.exception
 *
 * @author alexclin
 * @date 16/1/1 11:01
 */
public class HttpException extends Exception{
    private int code;
    private String message;

    public HttpException(int code,String detailMessage) {
        super(detailMessage);
        this.code = code;
        this.message = detailMessage;
    }

    public int getCode(){
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "HttpException{" +
                "code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
