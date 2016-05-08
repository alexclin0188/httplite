package alexclin.httplite.sample.retrofit.custom;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Query 作用与HttpLite库中的Param一样，只是用作示例
 *
 * @author alexclin  16/5/5 23:16
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Query {
    String value();
    boolean encoded() default false;
}
