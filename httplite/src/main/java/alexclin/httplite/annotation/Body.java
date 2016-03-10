package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Body
 *
 * @author alexclin 16/1/20 21:47
 */
@Documented
@Retention(RUNTIME)
@Target(PARAMETER)
public @interface Body {
    String value() default "";
}
