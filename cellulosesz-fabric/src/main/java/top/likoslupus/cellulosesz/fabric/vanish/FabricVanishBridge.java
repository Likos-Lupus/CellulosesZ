package top.likoslupus.cellulosesz.fabric.vanish;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

public final class FabricVanishBridge {

    private static final Set<UUID> VANISHED = ConcurrentHashMap.newKeySet();
    private static volatile BiPredicate<ServerPlayer, ServerPlayer> visibility = (_, _) -> false;

    private FabricVanishBridge() {
    }

    public static void visibility(BiPredicate<ServerPlayer, ServerPlayer> predicate) {
        visibility = predicate;
    }

    public static void vanished(UUID uuid, boolean vanished) {
        if (vanished) {
            VANISHED.add(uuid);
        } else {
            VANISHED.remove(uuid);
        }
    }

    public static boolean vanished(UUID uuid) {
        return VANISHED.contains(uuid);
    }

    public static void clear() {
        VANISHED.clear();
        visibility = (_, _) -> false;
    }

    public static boolean hiddenFrom(ServerPlayer viewer, ServerPlayer target) {
        return !viewer.getUUID().equals(target.getUUID())
                && VANISHED.contains(target.getUUID())
                && !visibility.test(viewer, target);
    }

}
