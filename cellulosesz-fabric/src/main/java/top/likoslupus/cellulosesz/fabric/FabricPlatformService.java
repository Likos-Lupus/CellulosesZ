package top.likoslupus.cellulosesz.fabric;

import com.google.gson.JsonParser;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.item.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.MovementSpeedType;
import top.likoslupus.cellulosesz.api.platform.PlatformCapability;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.fabric.display.FabricDisplayNameBridge;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;
import top.likoslupus.cellulosesz.fabric.vanish.FabricVanishBridge;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.requireNonNull;

public final class FabricPlatformService implements PlatformService, AutoCloseable {

    private static final Set<PlatformCapability> CAPABILITIES = Set.copyOf(EnumSet.allOf(PlatformCapability.class));
    private final FabricVanillaCommandBridge vanillaCommands;
    private final ExecutorService backupExecutor = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().name("cellulosesz-backup-", 0).factory()
    );
    private final AtomicBoolean backupRunning = new AtomicBoolean();
    private @Nullable MinecraftServer server;
    private @Nullable MessageRenderer renderer;
    private @Nullable LocaleResolver locales;

    public FabricPlatformService(FabricVanillaCommandBridge vanillaCommands) {
        this.vanillaCommands = vanillaCommands;
    }

    @Override
    public Set<PlatformCapability> capabilities() {
        return CAPABILITIES;
    }

    @Override
    public void runOnServerThread(Runnable task) {
        var current = requireNonNull(server, "Server has not started");
        if (current.isSameThread()) {
            task.run();
        } else {
            current.execute(task);
        }
    }

    @Override
    public <T> CompletableFuture<T> callOnServerThread(Supplier<T> task) {
        var future = new CompletableFuture<T>();
        runOnServerThread(() -> {
            try {
                future.complete(task.get());
            } catch (Throwable failure) {
                future.completeExceptionally(failure);
            }
        });
        return future;
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
    public Optional<String> address(CellPlayer player) {
        var remote = requireNative(player).connection.getRemoteAddress();
        if (remote instanceof java.net.InetSocketAddress socketAddress) {
            return Optional.of(socketAddress.getAddress().getHostAddress());
        }

        return Optional.of(remote.toString());
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
        var targetLevel = level(
                world.isBlank()
                        ? defaultWorld()
                        : world
        ).orElse(null);
        if (targetLevel == null) return false;

        targetLevel.setDayTime(Math.floorMod(time, 24000L));
        return true;
    }

    @Override
    public boolean setWeather(
            String world,
            String weather,
            int seconds
    ) {
        var targetLevel = level(
                world.isBlank()
                        ? defaultWorld()
                        : world
        ).orElse(null);
        if (targetLevel == null) return false;

        var requestedTicks = Math.max(1L, (long) seconds * 20L);
        var ticks = (int) Math.min(Integer.MAX_VALUE, requestedTicks);
        switch (weather.trim().toLowerCase(Locale.ROOT)) {
            case "clear" -> targetLevel.setWeatherParameters(ticks, 0, false, false);
            case "rain" -> targetLevel.setWeatherParameters(0, ticks, true, false);
            case "thunder" -> targetLevel.setWeatherParameters(0, ticks, true, true);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean setGameMode(CellPlayer player, String gameMode) {
        return switch (gameMode.trim().toLowerCase(Locale.ROOT)) {
            case "survival", "s", "0" -> requireNative(player).setGameMode(GameType.SURVIVAL);
            case "creative", "c", "1" -> requireNative(player).setGameMode(GameType.CREATIVE);
            case "adventure", "a", "2" -> requireNative(player).setGameMode(GameType.ADVENTURE);
            case "spectator", "sp", "3" -> requireNative(player).setGameMode(GameType.SPECTATOR);
            default -> false;
        };
    }

    @Override
    public Optional<String> gameMode(CellPlayer player) {
        return Optional.of(requireNative(player).gameMode().getName());
    }

    @Override
    public boolean flying(CellPlayer player) {
        return requireNative(player).getAbilities().flying;
    }

    @Override
    public boolean setMovementSpeed(CellPlayer player, MovementSpeedType type, double speed) {
        if (!Double.isFinite(speed) || speed < 0.0001D || speed > 10.0D) return false;

        var nativePlayer = requireNative(player);
        var abilities = nativePlayer.getAbilities();
        var normalized = speed < 1.0D
                ? (type == MovementSpeedType.FLY ? 0.1D : 0.2D) * speed
                : (type == MovementSpeedType.FLY ? 0.1D : 0.2D)
                        + ((speed - 1.0D) / 9.0D) * (1.0D - (type == MovementSpeedType.FLY ? 0.1D : 0.2D));
        if (type == MovementSpeedType.FLY) {
            abilities.setFlyingSpeed((float) normalized);
        } else {
            abilities.setWalkingSpeed((float) normalized);
        }
        nativePlayer.onUpdateAbilities();
        return true;
    }

    @Override
    public boolean setPersonalTime(CellPlayer player, @Nullable Long time) {
        var nativePlayer = requireNative(player);
        var level = nativePlayer.level();
        var dayTime = time == null ? level.getDayTime() : time;

        nativePlayer.connection.send(new ClientboundSetTimePacket(
                level.getGameTime(), dayTime, time == null
        ));

        return true;
    }

    @Override
    public boolean setPersonalWeather(CellPlayer player, @Nullable String weather) {
        var nativePlayer = requireNative(player);

        if (weather == null || weather.equalsIgnoreCase("reset")) {
            var level = nativePlayer.level();
            if (level.isRaining()) {
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, level.getRainLevel(1.0F)));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, level.getThunderLevel(1.0F)));
            } else {
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
            }
            return true;
        }

        switch (weather.toLowerCase(Locale.ROOT)) {
            case "clear", "sun" -> {
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 0.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
            }
            case "rain" -> {
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0F));
            }
            case "thunder", "storm" -> {
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, 1.0F));
                nativePlayer.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 1.0F));
            }
            default -> {
                return false;
            }
        }

        return true;
    }

    @Override
    public CompletableFuture<Path> backup(Path destinationDirectory) {
        var current = requireNonNull(server, "Server has not started");
        if (!backupRunning.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("A backup is already running"));
        }

        return callOnServerThread(() -> {
            if (!current.saveEverything(false, true, true)) {
                throw new IllegalStateException("Minecraft reported that the world save failed");
            }
            return current.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }).thenCompose(worldRoot -> CompletableFuture.supplyAsync(
                () -> createBackup(worldRoot, destinationDirectory),
                backupExecutor
        )).whenComplete((ignored, failure) -> backupRunning.set(false));
    }

    private Path createBackup(Path worldRoot, Path destinationDirectory) {
        var destinationRoot = destinationDirectory.toAbsolutePath().normalize();
        Path temporary = null;
        try {
            Files.createDirectories(destinationRoot);
            var realWorldRoot = worldRoot.toRealPath();
            var realDestinationRoot = destinationRoot.toRealPath();
            var filename = "backup-%s-%s.zip".formatted(
                    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                            .format(LocalDateTime.now(ZoneOffset.UTC)),
                    UUID.randomUUID().toString().substring(0, 8)
            );
            var destination = destinationRoot.resolve(filename).normalize();
            if (!destination.startsWith(destinationRoot)) {
                throw new IOException("Backup destination escaped its configured directory");
            }
            temporary = Files.createTempFile(destinationRoot, ".backup-", ".zip.tmp");
            var excludedRoot = realDestinationRoot.startsWith(realWorldRoot) ? realDestinationRoot : null;

            try (
                    var output = Files.newOutputStream(temporary, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
                    var zip = new ZipOutputStream(output);
                    var paths = Files.walk(realWorldRoot)
            ) {
                paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                        .filter(path -> excludedRoot == null || !path.startsWith(excludedRoot))
                        .forEach(path -> addBackupEntry(realWorldRoot, path, zip));
            }

            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination);
            }
            temporary = null;
            return destination;
        } catch (BackupFailure exception) {
            throw new IllegalStateException("Unable to create server backup", exception.getCause());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to create server backup", exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void addBackupEntry(Path worldRoot, Path path, ZipOutputStream zip) {
        var relative = worldRoot.relativize(path).normalize();
        if (relative.isAbsolute() || relative.startsWith("..")) {
            throw new BackupFailure(new IOException("Unsafe backup entry: " + path));
        }
        var entryName = relative.toString().replace('\\', '/');
        if (entryName.isBlank() || entryName.startsWith("/") || entryName.contains("../")) {
            throw new BackupFailure(new IOException("Unsafe backup entry: " + entryName));
        }
        try {
            zip.putNextEntry(new ZipEntry(entryName));
            Files.copy(path, zip);
            zip.closeEntry();
        } catch (IOException exception) {
            throw new BackupFailure(exception);
        }
    }

    @Override
    public int removeEntities(
            String selector,
            CellPlayer origin,
            int radius
    ) {
        var nativePlayer = requireNative(origin);
        var normalized = selector.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank() || radius <= 0) return -1;

        var groupedSelector = Set.of(
                "all", "entities", "item", "items", "drops", "xp", "experience",
                "mob", "mobs", "monster", "monsters", "animal", "animals"
        ).contains(normalized);
        var exactType = groupedSelector
                ? Optional.<EntityType<?>>empty()
                : EntityType.byString(normalizeEntityType(normalized));
        if (!groupedSelector && exactType.isEmpty()) return -1;

        var maximumDistance = (double) radius * radius;
        var targets = StreamSupport.stream(nativePlayer.level().getAllEntities().spliterator(), false)
                .filter(entity -> !(entity instanceof ServerPlayer))
                .filter(entity -> entity.distanceToSqr(nativePlayer) <= maximumDistance)
                .filter(entity -> matchesRemovalSelector(entity, normalized, exactType))
                .toList();

        targets.forEach(Entity::discard);
        return targets.size();
    }

    @Override
    public boolean giveItem(
            CellPlayer player,
            String itemArgument,
            int count
    ) {
        if (itemArgument.isBlank() || count <= 0) return false;

        var parsed = parseItem(itemArgument);
        if (parsed.isEmpty()) return false;

        var nativePlayer = requireNative(player);
        var inventory = nativePlayer.getInventory();
        var template = parsed.orElseThrow().createItemStack(1, false);
        var before = countMatching(inventory, template);

        var remaining = count;
        while (remaining > 0) {
            var part = template.copyWithCount(Math.min(remaining, template.getMaxStackSize()));
            inventory.add(part);
            remaining -= Math.min(remaining, template.getMaxStackSize()) - part.getCount();
            if (!part.isEmpty()) break;
        }

        var inserted = countMatching(inventory, template) - before;
        if (inserted != count) {
            removeMatching(inventory, template, inserted);
            inventory.setChanged();
            return false;
        }

        inventory.setChanged();
        return true;
    }

    @Override
    public int countItem(CellPlayer player, String itemArgument) {
        if (itemArgument.isBlank()) return 0;

        var parsed = parseItem(itemArgument);
        if (parsed.isEmpty()) return 0;

        return countMatching(
                requireNative(player).getInventory(),
                parsed.orElseThrow().createItemStack(1, false)
        );
    }

    @Override
    public boolean takeItem(
            CellPlayer player,
            String itemArgument,
            int count
    ) {
        if (count <= 0 || itemArgument.isBlank()) return false;

        var parsed = parseItem(itemArgument);
        if (parsed.isEmpty()) return false;

        var inventory = requireNative(player).getInventory();
        var template = parsed.orElseThrow().createItemStack(1, false);
        if (countMatching(inventory, template) < count) return false;

        var removed = removeMatching(inventory, template, count);
        inventory.setChanged();
        return removed == count;
    }

    @Override
    public Optional<String> heldItemId(CellPlayer player) {
        var stack = requireNative(player).getMainHandItem();
        return stack.isEmpty()
                ? Optional.empty()
                : Optional.of(itemId(stack));
    }

    @Override
    public boolean validItem(String itemId) {
        return !itemId.isBlank() && parseItem(normalizeItemId(itemId)).isPresent();
    }

    @Override
    public int maxStackSize(String itemId) {
        return parseItem(normalizeItemId(itemId))
                .map(input -> input.createItemStack(1, false).getMaxStackSize())
                .orElse(0);
    }

    @Override
    public boolean setHeldItemName(CellPlayer player, @Nullable String name) {
        var stack = requireNative(player).getMainHandItem();
        if (stack.isEmpty()) return false;

        if (name == null) {
            stack.remove(DataComponents.CUSTOM_NAME);
        } else if (name.isBlank()) {
            return false;
        } else {
            stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        }

        return true;
    }

    @Override
    public boolean setHeldItemLore(CellPlayer player, List<String> lore) {
        var stack = requireNative(player).getMainHandItem();
        if (stack.isEmpty()) return false;

        if (lore.isEmpty()) {
            stack.remove(DataComponents.LORE);
        } else {
            stack.set(
                    DataComponents.LORE,
                    new ItemLore(lore.stream().map(Component::literal).toList())
            );
        }

        return true;
    }

    @Override
    public boolean setHeldItemComponent(CellPlayer player, String componentId, String rawValue) {
        var nativePlayer = requireNative(player);
        var held = nativePlayer.getMainHandItem();
        if (held.isEmpty() || componentId.isBlank() || rawValue.isBlank() || rawValue.length() > 32_768) return false;

        var componentLocation = ResourceLocation.tryParse(normalizeItemId(componentId));
        if (componentLocation == null || !componentCompatible(held, componentLocation)) return false;
        var component = componentType(componentId);
        if (component.isEmpty()) return false;

        var source = parseItem("%s[%s=%s]".formatted(itemId(held), normalizeItemId(componentId), rawValue));
        if (source.isEmpty()) return false;

        var replacement = held.copy();
        if (!copyComponent(source.orElseThrow().createItemStack(1, false), replacement, component.orElseThrow())) {
            return false;
        }
        nativePlayer.setItemInHand(InteractionHand.MAIN_HAND, replacement);
        return true;
    }

    @Override
    public boolean clearHeldItemComponent(CellPlayer player, String componentId) {
        var nativePlayer = requireNative(player);
        var held = nativePlayer.getMainHandItem();
        var componentLocation = ResourceLocation.tryParse(normalizeItemId(componentId));
        var component = componentType(componentId);
        if (held.isEmpty() || componentLocation == null || !componentCompatible(held, componentLocation) || component.isEmpty())
            return false;

        var replacement = held.copy();
        replacement.remove(component.orElseThrow());
        nativePlayer.setItemInHand(InteractionHand.MAIN_HAND, replacement);
        return true;
    }

    @Override
    public boolean openWorkstation(CellPlayer player, String workstation) {
        var nativePlayer = requireNative(player);
        if (!nativePlayer.isAlive() || nativePlayer.hasDisconnected()) return false;
        var normalized = workstation.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "anvil" -> openWorkstationMenu(nativePlayer, MenuType.ANVIL, "anvil");
            case "cartography", "cartographytable" ->
                    openWorkstationMenu(nativePlayer, MenuType.CARTOGRAPHY_TABLE, "cartography");
            case "grindstone" -> openWorkstationMenu(nativePlayer, MenuType.GRINDSTONE, "grindstone");
            case "loom" -> openWorkstationMenu(nativePlayer, MenuType.LOOM, "loom");
            case "smithing", "smithingtable" -> openWorkstationMenu(nativePlayer, MenuType.SMITHING, "smithing");
            case "workbench", "crafting" -> openWorkstationMenu(nativePlayer, MenuType.CRAFTING, "crafting");
            case "stonecutter" -> openWorkstationMenu(nativePlayer, MenuType.STONECUTTER, "stonecutter");
            case "disposal" -> openDisposal(nativePlayer);
            default -> false;
        };
    }

    private boolean openWorkstationMenu(ServerPlayer player, MenuType<?> type, String translationSuffix) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> requireNonNull(type.create(id, inventory)),
                Component.translatable("container." + translationSuffix)
        ));
        return true;
    }

    private boolean openDisposal(ServerPlayer player) {
        var disposal = new SimpleContainer(27) {
            @Override
            public void stopOpen(ContainerUser user) {
                clearContent();
                super.stopOpen(user);
            }
        };
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, ignored) -> ChestMenu.threeRows(id, inventory, disposal),
                Component.translatable("container.chest")
        ));
        return true;
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
    public Optional<List<InventoryItemSnapshot>> inventorySnapshot(CellPlayer player) {
        var current = server;
        if (current == null) return Optional.empty();

        var inventory = requireNative(player).getInventory();
        var snapshots = new ArrayList<InventoryItemSnapshot>();
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty()) continue;

            var encoded = encodeItemStack(stack);
            if (encoded.isEmpty()) return Optional.empty();

            snapshots.add(new InventoryItemSnapshot(slot, encoded.orElseThrow()));
        }

        return Optional.of(List.copyOf(snapshots));
    }

    @Override
    public Optional<InventoryItemSnapshot> heldInventorySnapshot(CellPlayer player) {
        var inventory = requireNative(player).getInventory();
        var slot = inventory.getSelectedSlot();
        var stack = inventory.getItem(slot);
        if (stack.isEmpty()) return Optional.empty();
        return encodeItemStack(stack).map(encoded -> new InventoryItemSnapshot(slot, encoded));
    }

    @Override
    public Optional<ItemDescriptor> describeInventoryItem(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        return decodeItemStack(snapshot.validatedStack())
                .filter(stack -> !stack.isEmpty())
                .map(stack -> new ItemDescriptor(itemId(stack), stack.getCount()));
    }

    @Override
    public boolean plainInventoryItem(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        return decodeItemStack(snapshot.validatedStack())
                .filter(stack -> !stack.isEmpty())
                .map(stack -> ItemStack.isSameItemSameComponents(
                        stack,
                        new ItemStack(stack.getItem(), stack.getCount())
                ))
                .orElse(false);
    }

    @Override
    public Optional<InventoryGrant> prepareInventoryGrant(
            CellPlayer player,
            List<? extends InventoryItemSnapshot> snapshots
    ) {
        requireNonNull(snapshots, "snapshots");
        if (snapshots.isEmpty()) return Optional.empty();

        var inventory = requireNative(player).getInventory();
        var planned = new LinkedHashMap<Integer, ItemStack>();
        var before = new LinkedHashMap<Integer, ItemStack>();
        for (var snapshot : snapshots) {
            requireNonNull(snapshot, "snapshot");
            if (snapshot.slot < 0 || snapshot.slot >= inventory.getContainerSize()) {
                return Optional.empty();
            }

            if (planned.containsKey(snapshot.slot)) {
                return Optional.empty();
            }

            var decoded = decodeItemStack(snapshot.validatedStack());
            if (decoded.isEmpty() || decoded.orElseThrow().isEmpty()) {
                return Optional.empty();
            }

            var current = inventory.getItem(snapshot.slot);
            if (!current.isEmpty()) {
                return Optional.empty();
            }

            planned.put(snapshot.slot, decoded.orElseThrow().copy());
            before.put(snapshot.slot, current.copy());
        }

        return Optional.of(new InventoryGrant() {
            private boolean committed;

            @Override
            public boolean commit() {
                synchronized (inventory) {
                    if (committed) return false;

                    for (var entry : before.entrySet()) {
                        if (!sameStack(inventory.getItem(entry.getKey()), entry.getValue())) {
                            return false;
                        }
                    }

                    planned.forEach((slot, stack) ->
                            inventory.setItem(slot, stack.copy())
                    );
                    inventory.setChanged();
                    committed = true;
                    return true;
                }
            }

            @Override
            public boolean rollback() {
                synchronized (inventory) {
                    if (!committed) return true;

                    for (var entry : planned.entrySet()) {
                        if (!sameStack(inventory.getItem(entry.getKey()), entry.getValue())) {
                            return false;
                        }
                    }

                    before.forEach((slot, stack) ->
                            inventory.setItem(slot, stack.copy())
                    );
                    inventory.setChanged();
                    committed = false;
                    return true;
                }
            }
        });
    }

    @Override
    public Optional<InventoryMutation> prepareInventoryRemoval(
            CellPlayer player,
            List<InventoryStackSelection> selections
    ) {
        requireNonNull(selections, "selections");
        if (selections.isEmpty()) return Optional.empty();

        var inventory = requireNative(player).getInventory();
        var before = copyInventory(inventory);
        var after = copyStacks(before);
        var seen = new HashSet<Integer>();
        for (var selection : selections) {
            requireNonNull(selection, "selection");
            var slot = selection.snapshot().slot;
            if (slot < 0 || slot >= after.size() || !seen.add(slot)) return Optional.empty();

            var expected = decodeItemStack(selection.snapshot().validatedStack());
            if (expected.isEmpty() || !sameStack(before.get(slot), expected.orElseThrow())) {
                return Optional.empty();
            }
            var stack = after.get(slot);
            if (selection.count() > stack.getCount()) return Optional.empty();
            if (selection.count() == stack.getCount()) after.set(slot, ItemStack.EMPTY);
            else stack.shrink(selection.count());
        }
        return Optional.of(preparedInventoryMutation(inventory, before, after, Set.copyOf(seen)));
    }

    @Override
    public Optional<InventoryMutation> prepareInventoryExchange(
            CellPlayer player,
            List<InventoryItemRequest> removals,
            List<InventoryItemRequest> additions
    ) {
        requireNonNull(removals, "removals");
        requireNonNull(additions, "additions");
        if (removals.isEmpty() && additions.isEmpty()) return Optional.empty();

        var inventory = requireNative(player).getInventory();
        var before = copyInventory(inventory);
        var after = copyStacks(before);
        for (var request : removals) {
            var parsed = parseItem(request.itemArgument());
            if (parsed.isEmpty()) return Optional.empty();
            var template = parsed.orElseThrow().createItemStack(1, false);
            if (!removeMatching(after, template, request.count())) return Optional.empty();
        }
        for (var request : additions) {
            var parsed = parseItem(request.itemArgument());
            if (parsed.isEmpty()) return Optional.empty();
            var template = parsed.orElseThrow().createItemStack(1, false);
            if (!addMatching(after, template, request.count())) return Optional.empty();
        }
        var affected = IntStream.range(0, before.size())
                .filter(slot -> !sameStack(before.get(slot), after.get(slot)))
                .boxed()
                .collect(java.util.stream.Collectors.toSet());
        if (affected.isEmpty()) return Optional.empty();
        return Optional.of(preparedInventoryMutation(inventory, before, after, affected));
    }

    private boolean removeMatching(List<ItemStack> stacks, ItemStack template, int requested) {
        if (requested <= 0) return false;
        var remaining = requested;
        for (var slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            var stack = stacks.get(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) continue;
            var removed = Math.min(remaining, stack.getCount());
            if (removed == stack.getCount()) stacks.set(slot, ItemStack.EMPTY);
            else stack.shrink(removed);
            remaining -= removed;
        }
        return remaining == 0;
    }

    private boolean addMatching(List<ItemStack> stacks, ItemStack template, int requested) {
        if (requested <= 0) return false;
        var remaining = requested;
        for (var stack : stacks) {
            if (remaining == 0) break;
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) continue;
            var capacity = Math.max(0, Math.min(stack.getMaxStackSize(), template.getMaxStackSize()) - stack.getCount());
            var inserted = Math.min(capacity, remaining);
            if (inserted > 0) {
                stack.grow(inserted);
                remaining -= inserted;
            }
        }
        for (var slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            if (!stacks.get(slot).isEmpty()) continue;
            var inserted = Math.min(remaining, template.getMaxStackSize());
            stacks.set(slot, template.copyWithCount(inserted));
            remaining -= inserted;
        }
        return remaining == 0;
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
        var current = server;
        var stack = requireNative(player).getMainHandItem();
        if (current == null || enchantment.isBlank() || level <= 0 || stack.isEmpty()) {
            return false;
        }

        var location = ResourceLocation.tryParse(normalizeItemId(enchantment));
        if (location == null) return false;

        var registry = current.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var value = registry.getValue(location);
        if (value == null) return false;

        var mutable = new ItemEnchantments.Mutable(
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
        );
        mutable.set(registry.wrapAsHolder(value), level);
        stack.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
        return true;
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
    public boolean validEntityType(String entityType) {
        if (entityType.isBlank()) return false;

        var type = EntityType.byString(normalizeEntityType(entityType)).orElse(null);
        return type != null && type.canSummon() && type != EntityType.PLAYER;
    }

    @Override
    public int spawnMob(CellPlayer player, String entityType, int count) {
        var nativePlayer = requireNative(player);
        if (count <= 0 || count > 64 || entityType.isBlank()) return 0;

        var type = EntityType.byString(normalizeEntityType(entityType)).orElse(null);
        if (type == null || !validEntityType(entityType)) return 0;

        var serverLevel = nativePlayer.level();
        var position = nativePlayer.blockPosition().relative(nativePlayer.getDirection(), 2);
        return (int) IntStream.range(0, count)
                .takeWhile(_ -> type.spawn(serverLevel, position, EntitySpawnReason.COMMAND) != null)
                .count();
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
        return executeCommand(
                command,
                requireNative(player).createCommandSourceStack()
        ).isPresent();
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
    public boolean replaceSignText(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> expectedLines,
            List<String> replacementLines
    ) {
        if (expectedLines.size() != 4 || replacementLines.size() != 4) return false;
        var current = server;
        if (current == null || !current.isSameThread()) return false;
        var targetLevel = level(location.world);
        if (targetLevel.isEmpty()) return false;
        var position = blockPosition(location);
        var blockEntity = targetLevel.orElseThrow().getBlockEntity(position);
        if (!(blockEntity instanceof SignBlockEntity sign)) return false;
        if (!sameSignLines(sign, front, expectedLines)) return false;

        SignText text = front ? sign.getFrontText() : sign.getBackText();
        for (int line = 0; line < 4; line++) {
            text = text.setMessage(line, signComponent(replacementLines.get(line)));
        }
        if (!sign.setText(text, front)) return false;
        sign.setChanged();
        var state = targetLevel.orElseThrow().getBlockState(position);
        targetLevel.orElseThrow().sendBlockUpdated(position, state, state, 3);
        return true;
    }

    @Override
    public boolean breakSignBlock(
            CellPlayer player,
            CellLocation location,
            List<String> expectedFrontLines,
            List<String> expectedBackLines
    ) {
        if (expectedFrontLines.size() != 4 || expectedBackLines.size() != 4) return false;
        var current = server;
        if (current == null || !current.isSameThread()) return false;
        var targetLevel = level(location.world);
        if (targetLevel.isEmpty()) return false;
        var position = blockPosition(location);
        var blockEntity = targetLevel.orElseThrow().getBlockEntity(position);
        if (!(blockEntity instanceof SignBlockEntity sign)) return false;
        if (!sameSignLines(sign, true, expectedFrontLines)
                || !sameSignLines(sign, false, expectedBackLines)) return false;
        var nativePlayer = requireNative(player);
        return FabricPlatformEventBridge.withoutSignBreakCheck(() ->
                targetLevel.orElseThrow().destroyBlock(position, true, nativePlayer)
        );
    }

    @Override
    public top.likoslupus.cellulosesz.api.platform.NativeCommandResult dispatchNativeConsoleCommand(String command) {
        if (server == null) {
            return top.likoslupus.cellulosesz.api.platform.NativeCommandResult.notAvailable("Server has not started");
        }
        return vanillaCommands.execute(command, server.createCommandSourceStack());
    }

    private BlockPos blockPosition(CellLocation location) {
        if (!Double.isFinite(location.x) || !Double.isFinite(location.y) || !Double.isFinite(location.z)) {
            throw new IllegalArgumentException("Sign location must contain finite coordinates");
        }
        return BlockPos.containing(location.x, location.y, location.z);
    }

    private boolean sameSignLines(SignBlockEntity sign, boolean front, List<String> expected) {
        var actual = (front ? sign.getFrontText() : sign.getBackText()).getMessages(false);
        if (actual.length != expected.size()) return false;
        for (int index = 0; index < actual.length; index++) {
            if (!normalizeSignLine(actual[index].getString()).equals(normalizeSignLine(expected.get(index)))) {
                return false;
            }
        }
        return true;
    }

    private Component signComponent(String value) {
        if (value.length() >= 2 && value.charAt(0) == '§') {
            var formatting = ChatFormatting.getByCode(value.charAt(1));
            if (formatting != null) {
                return Component.literal(value.substring(2)).withStyle(formatting);
            }
        }
        return Component.literal(value);
    }

    private String normalizeSignLine(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC)
                .replaceAll("(?i)[§&][0-9A-FK-OR]", "")
                .strip();
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

    private boolean repair(ItemStack stack) {
        if (stack.isEmpty() || !stack.isDamageableItem() || stack.getDamageValue() <= 0) {
            return false;
        }
        stack.setDamageValue(0);
        return true;
    }

    private List<ItemStack> copyInventory(Container inventory) {
        var result = new ArrayList<ItemStack>(inventory.getContainerSize());
        for (var slot = 0; slot < inventory.getContainerSize(); slot++) {
            result.add(inventory.getItem(slot).copy());
        }
        return result;
    }

    private List<ItemStack> copyStacks(List<ItemStack> source) {
        return source.stream().map(ItemStack::copy).toCollection(ArrayList::new);
    }

    private InventoryMutation preparedInventoryMutation(
            Container inventory,
            List<ItemStack> before,
            List<ItemStack> after,
            Set<Integer> affectedSlots
    ) {
        return new InventoryMutation() {
            private boolean committed;

            @Override
            public boolean commit() {
                synchronized (inventory) {
                    if (committed || !inventoryEquals(inventory, before, affectedSlots)) return false;
                    replaceInventory(inventory, after, affectedSlots);
                    committed = true;
                    return true;
                }
            }

            @Override
            public boolean rollback() {
                synchronized (inventory) {
                    if (!committed) return true;
                    if (!inventoryEquals(inventory, after, affectedSlots)) return false;
                    replaceInventory(inventory, before, affectedSlots);
                    committed = false;
                    return true;
                }
            }
        };
    }

    private boolean inventoryEquals(
            Container inventory,
            List<ItemStack> expected,
            Set<Integer> affectedSlots
    ) {
        if (inventory.getContainerSize() != expected.size()) return false;
        for (var slot : affectedSlots) {
            if (slot < 0 || slot >= expected.size()
                    || !sameStack(inventory.getItem(slot), expected.get(slot))) {
                return false;
            }
        }
        return true;
    }

    private void replaceInventory(
            Container inventory,
            List<ItemStack> replacement,
            Set<Integer> affectedSlots
    ) {
        for (var slot : affectedSlots) {
            inventory.setItem(slot, replacement.get(slot).copy());
        }
        inventory.setChanged();
    }

    private boolean sameStack(ItemStack first, ItemStack second) {
        if (first.isEmpty() || second.isEmpty()) {
            return first.isEmpty() && second.isEmpty();
        }

        return first.getCount() == second.getCount()
                && ItemStack.isSameItemSameComponents(first, second);
    }

    private Optional<ItemStack> decodeItemStack(String encoded) {
        var current = server;
        if (current == null || encoded.isBlank()) return Optional.empty();

        try {
            var operations = current.registryAccess().createSerializationContext(JsonOps.INSTANCE);
            return ItemStack.CODEC.parse(operations, JsonParser.parseString(encoded)).result();
        } catch (RuntimeException _) {
            return Optional.empty();
        }
    }

    private Optional<String> encodeItemStack(ItemStack stack) {
        var current = server;
        if (current == null || stack.isEmpty()) return Optional.empty();

        var operations = current.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        return ItemStack.CODEC.encodeStart(operations, stack)
                .result()
                .map(Object::toString);
    }

    private boolean componentCompatible(ItemStack stack, ResourceLocation component) {
        var item = itemId(stack);
        return switch (component.toString()) {
            case "minecraft:potion_contents" -> Set.of(
                    "minecraft:potion",
                    "minecraft:splash_potion",
                    "minecraft:lingering_potion",
                    "minecraft:tipped_arrow"
            ).contains(item);
            case "minecraft:fireworks" -> item.equals("minecraft:firework_rocket");
            default -> true;
        };
    }

    private Optional<DataComponentType<?>> componentType(String id) {
        if (id.isBlank()) return Optional.empty();
        var location = ResourceLocation.tryParse(normalizeItemId(id));
        if (location == null) return Optional.empty();
        return Optional.ofNullable(BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(location));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean copyComponent(
            ItemStack source,
            ItemStack target,
            DataComponentType<?> component
    ) {
        var value = source.get((DataComponentType) component);
        if (value == null) return false;

        target.set((DataComponentType) component, value);
        return true;
    }

    private String normalizeItemId(String value) {
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.indexOf(':') < 0
                ? "minecraft:%s".formatted(normalized)
                : normalized;
    }

    private String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private Optional<ItemInput> parseItem(String argument) {
        var current = server;
        if (current == null || argument.isBlank()) return Optional.empty();

        try {
            var reader = new StringReader(argument.trim());
            var parsed = ItemParser.parseForItem(current.registryAccess(), reader);
            if (reader.canRead()) return Optional.empty();

            return Optional.of(parsed);
        } catch (CommandSyntaxException _) {
            return Optional.empty();
        }
    }

    private int countMatching(Container inventory, ItemStack template) {
        return IntStream.range(0, inventory.getContainerSize())
                .mapToObj(inventory::getItem)
                .filter(stack -> !stack.isEmpty()
                        && ItemStack.isSameItemSameComponents(stack, template)
                )
                .mapToInt(ItemStack::getCount)
                .sum();
    }

    private int removeMatching(
            Container inventory,
            ItemStack template,
            int count
    ) {
        var remaining = Math.max(0, count);
        for (var slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            var stack = inventory.getItem(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, template)) continue;

            var removed = Math.min(remaining, stack.getCount());
            stack.shrink(removed);
            if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
            remaining -= removed;
        }

        return count - remaining;
    }

    private String normalizeEntityType(String type) {
        return type.indexOf(':') < 0
                ? "minecraft:%s".formatted(type)
                : type;
    }

    private boolean matchesRemovalSelector(
            Entity entity,
            String selector,
            Optional<EntityType<?>> exactType
    ) {
        if (exactType.isPresent()) return entity.getType() == exactType.orElseThrow();
        return switch (selector) {
            case "all", "entities" -> true;
            case "item", "items", "drops" -> entity instanceof ItemEntity;
            case "xp", "experience" -> entity instanceof ExperienceOrb;
            case "mob", "mobs" -> entity instanceof Mob;
            case "monster", "monsters" -> entity instanceof Monster;
            case "animal", "animals" -> entity instanceof Animal;
            default -> false;
        };
    }

    private RichText message(
            CellPlayer viewer,
            String key,
            Map<String, ?> placeholders
    ) {
        var currentRenderer = requireNonNull(renderer, "Message renderer is not initialized");
        var currentLocales = requireNonNull(locales, "Locale resolver is not initialized");
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


    MinecraftServer requireServer() {
        return requireNonNull(server, "Server has not started");
    }

    Optional<ServerLevel> serverLevel(String world) {
        return level(world);
    }

    ServerPlayer nativePlayer(CellPlayer player) {
        return requireNative(player);
    }

    CellPlayer wrapPlayer(ServerPlayer player) {
        return wrap(player);
    }

    Optional<ItemStack> decodeSnapshot(InventoryItemSnapshot snapshot) {
        requireNonNull(snapshot, "snapshot");
        return decodeItemStack(snapshot.validatedStack());
    }

    Optional<String> encodeStack(ItemStack stack) {
        return encodeItemStack(stack);
    }

    String stackItemId(ItemStack stack) {
        return itemId(stack);
    }

    boolean sameNativeStack(ItemStack first, ItemStack second) {
        return sameStack(first, second);
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
    public void close() {
        backupExecutor.shutdownNow();
    }

    private static final class BackupFailure extends RuntimeException {

        private BackupFailure(IOException cause) {
            super(cause);
        }

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
