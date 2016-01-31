package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * JsonField
 *
 * @author alexclin
 * @date 16/1/31 13:52
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface JsonField {
    String value();
}
