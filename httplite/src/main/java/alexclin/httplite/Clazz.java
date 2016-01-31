package alexclin.httplite;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import alexclin.httplite.listener.Callback;
import alexclin.httplite.util.LogUtil;
import alexclin.httplite.util.Util;

/**
 * Clazz
 *
 * @author alexclin
 * @date 16/1/29 20:29
 */
public abstract class Clazz<T> {

    private final Type _type;

    protected Clazz(){
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class<?>) { // sanity check, should never happen
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        }
        _type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type type(){
        return _type;
    }

    public static <T> Clazz<T> of(Callback<T> callback){
        final Type type = Util.type(Callback.class,callback);
        return new Clazz<T>() {
            @Override
            public Type type() {
                return type;
            }
        };
    }
}
