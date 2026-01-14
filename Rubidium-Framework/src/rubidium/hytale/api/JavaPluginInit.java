package rubidium.hytale.api;

import java.lang.annotation.*;

/**
 * Annotation for plugin initialization metadata.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JavaPluginInit {
    
    String name();
    
    String version() default "1.0.0";
    
    String description() default "";
    
    String author() default "";
    
    String[] authors() default {};
    
    String[] depend() default {};
    
    String[] softDepend() default {};
    
    String website() default "";
}
