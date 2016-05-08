package alexclin.httplite.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * FixHeaders  copy from Retrofit-Headers
 *
 * FixHeaders("Cache-Control: max-age=640000")
 *
 * FixHeaders({
 *   "X-Foo: Bar",
 *   "X-Ping: Pong"
 * })
 *
 * @author alexclin  16/5/6 20:51
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface FixHeaders {
    String[] value();
}
