package alexclin.httplite.sample.retrofit.custom;

/**
 * Query 作用与HttpLite库中的Param一样，只是用作示例
 *
 * @author alexclin  16/5/5 23:16
 */
public @interface Query {
    String value();
    boolean encoded() default false;
}
