package alexclin.httplite.sample.retrofit.custom;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * GsonField
 *
 * @author alexclin  16/5/5 23:10
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface GsonField {
    String value();
}
