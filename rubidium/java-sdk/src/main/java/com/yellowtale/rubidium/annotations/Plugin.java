package com.yellowtale.rubidium.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Plugin {
    
    String id();
    
    String name() default "";
    
    String version() default "1.0.0";
    
    String author() default "";
    
    String description() default "";
    
    String apiVersion() default "1.0.0";
    
    String[] dependencies() default {};
    
    String[] softDependencies() default {};
    
    String[] loadBefore() default {};
}
