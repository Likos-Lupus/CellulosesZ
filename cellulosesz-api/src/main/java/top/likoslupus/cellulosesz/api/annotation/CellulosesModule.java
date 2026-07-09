package top.likoslupus.cellulosesz.api.annotation;

import top.likoslupus.cellulosesz.api.module.ModulePhase;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CellulosesModule {

    String id();

    String name() default "";

    String description() default "";

    ModulePhase phase() default ModulePhase.FEATURE;

    int priority() default 0;

    String[] requires() default {};

    String[] optional() default {};

    boolean enabledByDefault() default true;

}
