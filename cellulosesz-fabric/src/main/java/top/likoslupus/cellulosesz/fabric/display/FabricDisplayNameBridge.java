package top.likoslupus.cellulosesz.fabric.display;

import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FabricDisplayNameBridge {

    private static final Map<UUID, Component> DISPLAY_NAMES = new ConcurrentHashMap<>();

    private FabricDisplayNameBridge() {
    }

    public static void displayName(UUID uuid, Component component) {
        DISPLAY_NAMES.put(uuid, component);
    }

    public static void clear(UUID uuid) {
        DISPLAY_NAMES.remove(uuid);
    }

    public static @Nullable Component displayName(UUID uuid) {
        return DISPLAY_NAMES.get(uuid);
    }

}
