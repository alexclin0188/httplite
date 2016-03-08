package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Form
 *
 * @author alexclin  16/1/20 21:43
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface Form {
    String value();

    /** Specifies whether the {@linkplain #value() value} and name are already URL encoded. */
    boolean encoded() default false;
}
