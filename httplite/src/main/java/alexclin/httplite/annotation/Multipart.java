package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Multipart
 *
 * @author alexclin
 * @date 16/1/20 21:50
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Multipart {
    String value() default "";
}
