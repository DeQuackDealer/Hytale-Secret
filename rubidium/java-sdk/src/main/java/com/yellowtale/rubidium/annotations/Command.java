package com.yellowtale.rubidium.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    
    String name();
    
    String[] aliases() default {};
    
    String description() default "";
    
    String usage() default "";
    
    String permission() default "";
    
    String permissionMessage() default "You don't have permission to use this command.";
    
    boolean playerOnly() default false;
    
    int minArgs() default 0;
    
    int maxArgs() default -1;
}
