package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * IntoFile
 *
 * @author alexclin
 * @date 16/1/30 09:47
 */
@Documented
@Target(PARAMETER)
@Retention(RUNTIME)
public @interface IntoFile {
    boolean resume() default true;

    boolean rename() default true;
}
