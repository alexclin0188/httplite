package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Tag
 *
 * @author alexclin  16/1/21 20:29
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Tag {
}
