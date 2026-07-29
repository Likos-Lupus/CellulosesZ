package top.likoslupus.cellulosesz.common.command;

/**
 * A module-owned command tree contribution. Implementations register Brigadier nodes directly.
 */
public interface CommandContributor {

    String moduleId();

    void register(CommandRegistrationContext context);

}
