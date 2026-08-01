package top.likoslupus.cellulosesz.fabric;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import java.util.Locale;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Shared loader-specific access to the attached Minecraft server.
 */
public final class FabricServerAccess {

    private @Nullable MinecraftServer server;

    public MinecraftServer requireServer() {
        return requireNonNull(server, "Server has not started");
    }

    public boolean serverThread() {
        var current = server;
        return current != null && current.isSameThread();
    }

    public ServerPlayer player(CellPlayer player) {
        return MinecraftPlayers.requireOnline(requireNonNull(player, "player"));
    }

    public Optional<ServerLevel> level(String worldId) {
        var current = server;
        if (current == null || worldId.isBlank()) {
            return Optional.empty();
        }

        var normalized = normalizeWorldId(worldId);
        return StreamSupport.stream(current.getAllLevels().spliterator(), false)
                .filter(level -> normalizeWorldId(
                        level.dimension().identifier().toString()
                ).equals(normalized))
                .findFirst();
    }

    private static String normalizeWorldId(String worldId) {
        var normalized = worldId.strip().toLowerCase(Locale.ROOT);
        return normalized.contains(":")
                ? normalized
                : "minecraft:" + normalized;
    }

    public String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    public void attach(MinecraftServer server) {
        if (this.server != null) {
            throw new IllegalStateException("A Minecraft server is already attached");
        }

        this.server = requireNonNull(server, "server");
    }

    public void detach(MinecraftServer server) {
        if (this.server != requireNonNull(server, "server")) {
            throw new IllegalStateException("Cannot detach a different Minecraft server");
        }

        this.server = null;
    }

    public void clear() {
        server = null;
    }

}
