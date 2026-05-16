package tak.server.plugins;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Compile-only stub of {@code tak.server.plugins.TakServerPlugin}. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TakServerPlugin {
    String name() default "";
    String description() default "";
}
