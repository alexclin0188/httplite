package alexclin.httplite.retrofit;


import alexclin.httplite.HttpLite;

/**
 * MethodFilter
 *
 * @author alexclin 16/1/31 00:05
 */
public interface MethodFilter {
    Object onMethod(HttpLite lite,MethodInvoker invoker,Object[] args) throws Throwable;
}
