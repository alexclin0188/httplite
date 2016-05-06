package alexclin.httplite.retrofit;


import java.lang.reflect.Method;

import alexclin.httplite.listener.RequestListener;

/**
 * MethodInvoker
 *
 * @author alexclin  16/4/12 20:31
 */
public class MethodInvoker {
    private MethodHandler<?> methodHandler;
    private Method method;
    private Retrofit retrofit;
    private RequestListener filter;

    MethodInvoker(MethodHandler<?> methodHandler, Method method, Retrofit retrofit, RequestListener filter) {
        this.methodHandler = methodHandler;
        this.method = method;
        this.retrofit = retrofit;
        this.filter = filter;
    }

    public Method method(){
        return method;
    }

    public Object invoke(Object[] args) throws Throwable{
        return methodHandler.invoke(retrofit,filter,args);
    }
}
