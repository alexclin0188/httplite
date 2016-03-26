package alexclin.httplite;

/**
 * ResultParser
 *
 * @author alexclin  16/3/24 22:23
 */
public interface ResultParser {
    <T> T parseResult(Response response) throws Exception;
}
