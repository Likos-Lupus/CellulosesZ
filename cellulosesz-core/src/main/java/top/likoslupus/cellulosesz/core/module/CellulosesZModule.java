package top.likoslupus.cellulosesz.core.module;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

    default CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext context) {
        return CompletableFuture.completedFuture(PreparedReloads.noop());
    }

    default void onUnload(ModuleContext context) {
    }

    default void onServerStopping(ModuleContext context) {
    }

}
