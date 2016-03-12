package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * BaseURL
 *
 * @author alexclin  16/3/12 13:52
 */
@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface BaseURL {
    String value();
}
