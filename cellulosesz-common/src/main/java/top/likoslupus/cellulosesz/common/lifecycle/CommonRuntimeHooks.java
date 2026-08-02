package top.likoslupus.cellulosesz.common.lifecycle;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Narrow loader adapter for behavior that has no common implementation yet.
 */
public interface CommonRuntimeHooks extends AutoCloseable {

    default void initialize() {
    }

    default void beforeServerTick(MinecraftServer server) {
    }

    default void afterServerTick(MinecraftServer server) {
    }

    default void afterPlayerJoin(ServerPlayer player) {
    }

    default void afterPlayerQuit(ServerPlayer player) {
    }

    @Override
    void close();

}
