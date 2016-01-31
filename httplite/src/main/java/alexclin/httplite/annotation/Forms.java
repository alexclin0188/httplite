package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Forms
 *
 * @author alexclin
 * @date 16/1/20 21:44
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Forms {
    /** Specifies whether the names and values are already URL encoded. */
    boolean encoded() default false;
}
