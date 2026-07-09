package top.likoslupus.cellulosesz.api.annotation;

import top.likoslupus.cellulosesz.api.command.CommandSourceKind;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CellulosesCommand {

    String name();

    String[] aliases() default {};

    String permission() default "";

    CommandSourceKind source() default CommandSourceKind.ANY;

    String description() default "";

    String usage() default "";

    String module() default "";

}
