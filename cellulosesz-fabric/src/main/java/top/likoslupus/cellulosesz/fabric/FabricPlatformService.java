package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.ItemDescriptor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public final class FabricPlatformService implements PlatformService {

    private final FabricVanillaCommandBridge vanillaCommands;
    private @Nullable MinecraftServer server;
    private @Nullable MessageRenderer renderer;
    private @Nullable LocaleResolver locales;

    public FabricPlatformService(FabricVanillaCommandBridge vanillaCommands) {
        this.vanillaCommands = vanillaCommands;
    }

    public void server(MinecraftServer server) {
        this.server = server;
    }

    public void messages(
            MessageRenderer renderer,
            LocaleResolver locales
    ) {
        this.renderer = renderer;
        this.locales = locales;
    }

    @Override
    public Optional<CellPlayer> player(CommandInvocation invocation) {
        if (invocation.nativeSource() instanceof CommandSourceStack source) {
            return player(source.getEntity());
        }
        return Optional.empty();
    }

    @Override
    public Optional<CellPlayer> player(Object nativeHandle) {
        if (nativeHandle instanceof ServerPlayer player) {
            return Optional.of(wrap(player));
        }
        return Optional.empty();
    }

    @Override
    public Optional<CellPlayer> onlinePlayer(String name) {
        if (server == null || name.isBlank()) return Optional.empty();

        var exact = server.getPlayerList().getPlayerByName(name);
        if (exact != null) return Optional.of(wrap(exact));

        return server.getPlayerList().getPlayers().stream()
                .filter(player -> player.getGameProfile().name().equalsIgnoreCase(name))
                .findFirst()
                .map(this::wrap);
    }

    @Override
    public List<CellPlayer> onlinePlayers() {
        if (server == null) return List.of();
        return server.getPlayerList().getPlayers().stream()
                .map(this::wrap)
                .toList();
    }

    @Override
    public List<String> worlds() {
        if (server == null) return List.of();
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .map(level -> level.dimension().identifier().toString())
                .toList();
    }

    @Override
    public String defaultWorld() {
        if (server == null) return "minecraft:overworld";
        return Level.OVERWORLD.identifier().toString();
    }

    @Override
    public CellLocation location(CellPlayer player) {
        var nativePlayer = requireNative(player);
        return new CellLocation(
                nativePlayer.level().dimension().identifier().toString(),
                nativePlayer.getX(),
                nativePlayer.getY(),
                nativePlayer.getZ(),
                nativePlayer.getYRot(),
                nativePlayer.getXRot()
        );
    }

    @Override
    public CompletableFuture<Boolean> teleport(CellPlayer player, CellLocation location) {
        return CompletableFuture.completedFuture(teleportNow(player, location));
    }

    @Override
    public Optional<CellLocation> safeLocation(CellLocation location) {
        var level = level(location.world);
        if (level.isEmpty()) return Optional.empty();

        var base = BlockPos.containing(location.x, location.y, location.z);
        for (int dy = 0; dy <= 8; dy++) {
            var up = base.above(dy);
            if (safe(level.get(), up)) {
                return Optional.of(new CellLocation(
                        location.world,
                        up.getX() + 0.5D,
                        up.getY(),
                        up.getZ() + 0.5D,
                        location.yaw,
                        location.pitch
                ));
            }
        }
        for (int dy = 1; dy <= 8; dy++) {
            var down = base.below(dy);
            if (safe(level.get(), down)) {
                return Optional.of(new CellLocation(
                        location.world,
                        down.getX() + 0.5D,
                        down.getY(),
                        down.getZ() + 0.5D,
                        location.yaw,
                        location.pitch
                ));
            }
        }

        return highestLocation(location.world, location.x, location.z)
                .map(found -> new CellLocation(
                        found.world,
                        found.x,
                        found.y,
                        found.z,
                        location.yaw,
                        location.pitch
                ));
    }

    @Override
    public Optional<CellLocation> highestLocation(
            String world,
            double x,
            double z
    ) {
        return level(world).map(level -> {
            var top = level.getHeightmapPos(
                    Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(x, 0.0D, z)
            );
            return new CellLocation(
                    world,
                    top.getX() + 0.5D,
                    top.getY(),
                    top.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );
        });
    }

    @Override
    public Optional<CellLocation> targetLocation(CellPlayer player, int maxDistance) {
        var nativePlayer = requireNative(player);
        var block = nativePlayer.pick(maxDistance, 0.0F, false).getLocation();
        return Optional.of(new CellLocation(
                nativePlayer.level().dimension().identifier().toString(),
                block.x,
                block.y,
                block.z,
                nativePlayer.getYRot(),
                nativePlayer.getXRot()
        ));
    }

    @Override
    public void sendMessage(CellPlayer player, String message) {
        sendMessage(player, RichText.plain(message));
    }

    @Override
    public void sendMessage(CellPlayer player, RichText message) {
        requireNative(player).sendSystemMessage(FabricTextAdapter.toComponent(message));
    }

    @Override
    public String locale(CellPlayer player) {
        return requireNative(player).clientInformation().language();
    }

    @Override
    public void setDisplayName(CellPlayer player, RichText displayName) {
        var nativePlayer = requireNative(player);
        var component = FabricTextAdapter.toComponent(displayName);
        FabricDisplayNameBridge.displayName(player.uuid(), component);
        if (server != null) {
            var packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(nativePlayer));
            server.getPlayerList().getPlayers().stream()
                    .filter(viewer -> !FabricVanishBridge.hiddenFrom(viewer, nativePlayer))
                    .forEach(viewer -> viewer.connection.send(packet));
        }
    }

    @Override
    public void kick(CellPlayer player, String reason) {
        var rendered = reason.isBlank()
                ? message(player, "service.admin.kick-default", Map.of()).plainText()
                : reason;
        requireNative(player).connection.disconnect(Component.literal(rendered));
    }

    @Override
    public boolean setFlying(CellPlayer player, boolean enabled) {
        var nativePlayer = requireNative(player);
        var abilities = nativePlayer.getAbilities();
        abilities.mayfly = enabled || nativePlayer.isCreative() || nativePlayer.isSpectator();
        abilities.flying = enabled;
        nativePlayer.onUpdateAbilities();
        return true;
    }

    @Override
    public boolean setInvulnerable(CellPlayer player, boolean enabled) {
        var nativePlayer = requireNative(player);
        nativePlayer.getAbilities().invulnerable = enabled;
        nativePlayer.onUpdateAbilities();
        return true;
    }

    @Override
    public boolean heal(CellPlayer player) {
        var nativePlayer = requireNative(player);
        nativePlayer.setHealth(nativePlayer.getMaxHealth());
        nativePlayer.clearFire();
        return true;
    }

    @Override
    public boolean feed(CellPlayer player) {
        var nativePlayer = requireNative(player);
        nativePlayer.getFoodData().setFoodLevel(20);
        nativePlayer.getFoodData().setSaturation(20.0F);
        return true;
    }

    @Override
    public boolean setTime(String world, long time) {
        var targetLevel = level(world.isBlank() ? defaultWorld() : world).orElse(null);
        if (targetLevel == null || server == null) return false;

        return vanillaCommands.execute(
                "time set " + time,
                server.createCommandSourceStack().withLevel(targetLevel)
        ).isPresent();
    }

    @Override
    public boolean setWeather(
            String world,
            String weather,
            int seconds
    ) {
        var targetLevel = level(world.isBlank() ? defaultWorld() : world).orElse(null);
        if (targetLevel == null || server == null) return false;

        var type = switch (weather.toLowerCase()) {
            case "rain" -> "rain";
            case "thunder" -> "thunder";
            default -> "clear";
        };
        return vanillaCommands.execute(
                "weather " + type + " " + Math.max(1, seconds),
                server.createCommandSourceStack().withLevel(targetLevel)
        ).isPresent();
    }

    @Override
    public int removeEntities(
            String selector,
            CellPlayer origin,
            int radius
    ) {
        var target = removeSelector(selector, radius);
        if (target.isBlank()) return -1;

        return dispatchConsoleCommand("execute as " + origin.name() + " at @s run kill " + target) ? 0 : -1;
    }

    @Override
    public boolean giveItem(
            CellPlayer player,
            String itemArgument,
            int count
    ) {
        if (itemArgument.isBlank() || count <= 0 || server == null) return false;
        return vanillaCommands.execute(
                "give %s %s %d".formatted(player.name(), itemArgument, count),
                server.createCommandSourceStack()
        ).orElse(0) > 0;
    }

    @Override
    public int countItem(CellPlayer player, String itemArgument) {
        if (itemArgument.isBlank() || server == null) return 0;
        return Math.max(0, vanillaCommands.execute(
                "clear %s %s 0".formatted(player.name(), itemArgument),
                server.createCommandSourceStack()
        ).orElse(0));
    }

    @Override
    public boolean takeItem(
            CellPlayer player,
            String itemArgument,
            int count
    ) {
        if (count <= 0 || server == null || countItem(player, itemArgument) < count) return false;
        return vanillaCommands.execute(
                "clear %s %s %d".formatted(player.name(), itemArgument, count),
                server.createCommandSourceStack()
        ).orElse(0) == count;
    }

    @Override
    public Optional<String> heldItemId(CellPlayer player) {
        var stack = requireNative(player).getMainHandItem();
        return stack.isEmpty()
                ? Optional.empty()
                : Optional.of(itemId(stack));
    }

    @Override
    public List<ItemDescriptor> inventoryItems(CellPlayer player) {
        var inventory = requireNative(player).getInventory();
        var counts = new LinkedHashMap<String, Integer>();
        IntStream.range(0, inventory.getContainerSize())
                .mapToObj(inventory::getItem)
                .filter(stack -> !stack.isEmpty())
                .forEach(stack ->
                        counts.merge(itemId(stack), stack.getCount(), Integer::sum)
                );
        return counts.entrySet().stream()
                .map(entry -> new ItemDescriptor(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Override
    public void sendChatMessage(CellPlayer player, String message) {
        if (server == null || message.isBlank()) return;
        var nativePlayer = requireNative(player);
        server.getPlayerList().broadcastSystemMessage(
                Component.translatable(
                        "chat.type.text",
                        nativePlayer.getDisplayName(),
                        Component.literal(message)
                ),
                false
        );
    }

    @Override
    public boolean enchantHeldItem(
            CellPlayer player,
            String enchantment,
            int level
    ) {
        if (enchantment.isBlank()
                || level <= 0
                || requireNative(player).getMainHandItem().isEmpty()
        ) return false;
        var normalized = enchantment.indexOf(':') < 0
                ? "minecraft:%s".formatted(enchantment)
                : enchantment;
        if (server == null) return false;
        return vanillaCommands.execute(
                "enchant %s %s %d".formatted(player.name(), normalized, level),
                server.createCommandSourceStack()
        ).isPresent();
    }

    @Override
    public int repairItems(CellPlayer player, boolean all) {
        var nativePlayer = requireNative(player);
        if (!all) return repair(nativePlayer.getMainHandItem()) ? 1 : 0;

        var inventory = nativePlayer.getInventory();
        int repaired = (int) IntStream.range(0, inventory.getContainerSize())
                .filter(slot -> repair(inventory.getItem(slot)))
                .count();
        inventory.setChanged();
        return repaired;
    }

    @Override
    public boolean openInventory(CellPlayer viewer, CellPlayer target) {
        var viewerNative = requireNative(viewer);
        var targetNative = requireNative(target);
        var mirror = new InventoryMirror(targetNative.getInventory(), 54);
        viewerNative.openMenu(new SimpleMenuProvider(
                (id, inventory, _) -> ChestMenu.sixRows(id, inventory, mirror),
                FabricTextAdapter.toComponent(message(
                        viewer,
                        "platform.inventory.title",
                        Map.of("player", target.name())
                ))
        ));
        return true;
    }

    @Override
    public boolean openEnderChest(CellPlayer viewer, CellPlayer target) {
        var viewerNative = requireNative(viewer);
        var targetNative = requireNative(target);
        var enderChest = targetNative.getEnderChestInventory();
        viewerNative.openMenu(new SimpleMenuProvider(
                (id, inventory, _) -> ChestMenu.threeRows(id, inventory, enderChest),
                FabricTextAdapter.toComponent(message(
                        viewer,
                        "platform.ender-chest.title",
                        Map.of("player", target.name())
                ))
        ));
        return true;
    }

    @Override
    public boolean dispatchPlayerCommand(CellPlayer player, String command) {
        if (server == null || command.isBlank()) return false;
        return executeCommand(command, requireNative(player).createCommandSourceStack()).isPresent();
    }

    @Override
    public void maintainItemCount(
            CellPlayer player,
            String itemId,
            int minimum
    ) {
        var missing = Math.max(0, minimum - countItem(player, itemId));
        if (missing > 0) giveItem(player, normalizeItemId(itemId), missing);
    }

    @Override
    public void setPlayerVisible(
            CellPlayer viewer,
            CellPlayer target,
            boolean visible
    ) {
        var viewerNative = requireNative(viewer);
        var targetNative = requireNative(target);
        if (viewerNative.getUUID().equals(targetNative.getUUID())) return;

        if (visible) {
            viewerNative.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(targetNative)));
            targetNative.startSeenByPlayer(viewerNative);
        } else {
            viewerNative.connection.send(new ClientboundRemoveEntitiesPacket(targetNative.getId()));
            viewerNative.connection.send(new ClientboundPlayerInfoRemovePacket(List.of(targetNative.getUUID())));
            targetNative.stopSeenByPlayer(viewerNative);
        }
    }

    @Override
    public void setVanishedState(CellPlayer player, boolean vanished) {
        FabricVanishBridge.vanished(player.uuid(), vanished);
    }

    @Override
    public void refreshCommandTree() {
        var current = server;
        if (current == null) return;
        current.getPlayerList().getPlayers().forEach(player ->
                current.getCommands().sendCommands(player)
        );
    }

    @Override
    public boolean dispatchConsoleCommand(String command) {
        if (server == null || command.isBlank()) return false;
        return executeCommand(command, server.createCommandSourceStack()).isPresent();
    }

    @Override
    public boolean dispatchNativeConsoleCommand(String command) {
        if (server == null || command.isBlank()) return false;
        return vanillaCommands.execute(command, server.createCommandSourceStack()).isPresent();
    }

    private String normalizeItemId(String value) {
        var normalized = value.trim().toLowerCase();
        return normalized.indexOf(':') < 0
                ? "minecraft:%s".formatted(normalized)
                : normalized;
    }

    private boolean repair(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) return false;
        stack.setDamageValue(0);
        return true;
    }

    private String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private String removeSelector(String selector, int radius) {
        var distance = ",distance=.." + Math.max(1, radius);
        var normalized = selector.trim().toLowerCase();
        return switch (normalized) {
            case "all", "entities" -> "@e[type=!minecraft:player%s]".formatted(distance);
            case "item", "items", "drops" -> "@e[type=minecraft:item%s]".formatted(distance);
            case "xp", "experience" -> "@e[type=minecraft:experience_orb%s]".formatted(distance);
            case "mob", "mobs", "monsters" ->
                    "@e[type=!minecraft:player,type=!minecraft:item,type=!minecraft:experience_orb%s]".formatted(distance);
            default -> normalized.matches("[a-z0-9_.-]+(:[a-z0-9_./-]+)?")
                    ? "@e[type=%s%s]".formatted(normalizeEntityType(normalized), distance)
                    : "";
        };
    }

    private String normalizeEntityType(String type) {
        return type.indexOf(':') < 0
                ? "minecraft:%s".formatted(type)
                : type;
    }

    private OptionalInt executeCommand(String command, CommandSourceStack source) {
        if (server == null || command.isBlank()) return OptionalInt.empty();

        var normalized = command.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        if (normalized.isBlank()) return OptionalInt.empty();

        try {
            return OptionalInt.of(server.getCommands().getDispatcher().execute(normalized, source));
        } catch (CommandSyntaxException _) {
            return OptionalInt.empty();
        }
    }

    private RichText message(
            CellPlayer viewer,
            String key,
            Map<String, ?> placeholders
    ) {
        var currentRenderer = Objects.requireNonNull(renderer, "Message renderer is not initialized");
        var currentLocales = Objects.requireNonNull(locales, "Locale resolver is not initialized");
        return currentRenderer.render(currentLocales.locale(viewer), key, placeholders);
    }

    private boolean safe(ServerLevel level, BlockPos feet) {
        var below = feet.below();
        var head = feet.above();
        return !level.getBlockState(below).isAir()
                && level.getBlockState(feet).isAir()
                && level.getBlockState(head).isAir();
    }

    private boolean teleportNow(CellPlayer player, CellLocation location) {
        var nativePlayer = requireNative(player);
        var targetLevel = level(location.world).orElse(null);
        if (targetLevel == null) return false;

        return nativePlayer.teleportTo(
                targetLevel,
                location.x,
                location.y,
                location.z,
                Set.of(),
                location.yaw,
                location.pitch,
                true
        );
    }

    private Optional<ServerLevel> level(String world) {
        if (server == null || world.isBlank()) return Optional.empty();

        var normalized = normalizeWorldName(world);
        return StreamSupport.stream(server.getAllLevels().spliterator(), false)
                .filter(level -> normalizeWorldName(level.dimension().identifier().toString()).equals(normalized))
                .findFirst();
    }

    private String normalizeWorldName(String world) {
        var value = world.trim().toLowerCase();
        if (value.indexOf(':') < 0) {
            return "minecraft:%s".formatted(value);
        }
        return value;
    }

    private ServerPlayer requireNative(CellPlayer player) {
        if (player.nativeHandle() instanceof ServerPlayer nativePlayer) {
            return nativePlayer;
        }
        throw new IllegalArgumentException("CellPlayer does not wrap a ServerPlayer: " + player);
    }

    private CellPlayer wrap(ServerPlayer player) {
        return new CellPlayer(
                player.getUUID(),
                player.getGameProfile().name(),
                player
        );
    }

    private static final class InventoryMirror extends SimpleContainer {

        private final Container target;
        private final int mirroredSlots;
        private boolean loading = true;

        private InventoryMirror(
                Container target,
                int size
        ) {
            super(size);
            this.target = target;
            this.mirroredSlots = Math.min(target.getContainerSize(), size);
            IntStream.range(0, mirroredSlots)
                    .forEach(slot -> super.setItem(
                            slot,
                            target.getItem(slot).copy()
                    ));
            loading = false;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (!loading) synchronize();
        }

        private void synchronize() {
            IntStream.range(0, mirroredSlots)
                    .forEach(slot -> target.setItem(
                            slot, getItem(slot).copy()
                    ));
            target.setChanged();
        }

        @Override
        public void stopOpen(ContainerUser user) {
            synchronize();
            super.stopOpen(user);
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return slot >= 0 && slot < mirroredSlots;
        }

    }

}
