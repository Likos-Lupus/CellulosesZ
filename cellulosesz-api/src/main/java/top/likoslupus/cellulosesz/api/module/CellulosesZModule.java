package top.likoslupus.cellulosesz.api.module;

public interface CellulosesZModule {

    default void construct(ModuleContext context) {
    }

    default void registerConfigs(ModuleContext context) {
    }

    default void registerServices(ModuleContext context) {
    }

    default void registerEvents(ModuleContext context) {
    }

    default void registerCommands(ModuleContext context) {
    }

    default void onServerStarting(ModuleContext context) {
    }

    default void onServerStarted(ModuleContext context) {
    }

    default void onReload(ModuleContext context) {
    }

    default void onServerStopping(ModuleContext context) {
    }

}
