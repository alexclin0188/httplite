package alexclin.httplite;

/**
 * CallFactory
 *
 * @author alexclin
 * @date 16/1/29 21:57
 */
public interface CallFactory {
    Call newCall(Request request);
}
