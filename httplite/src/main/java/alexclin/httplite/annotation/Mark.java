package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Mark
 *
 * @author alexclin  16/3/12 19:37
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Mark {
    String value();
}
