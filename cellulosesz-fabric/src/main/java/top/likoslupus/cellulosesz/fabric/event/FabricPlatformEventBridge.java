package top.likoslupus.cellulosesz.fabric.event;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.event.*;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.*;

/**
 * Fabric-to-core event adapter. Minecraft/Fabric objects are converted at this boundary so feature modules only depend
 * on the platform-neutral DTOs in cellulosesz-api.
 */
public final class FabricPlatformEventBridge {

    private static @Nullable FabricPlatformEventBridge active;

    private final EventRegistry events;
    private final PlatformService platform;
    private final Map<UUID, PlayerSnapshot> snapshots = new HashMap<>();
    private final Set<UUID> commandRedispatch = new HashSet<>();

    public FabricPlatformEventBridge(
            EventRegistry events,
            PlatformService platform
    ) {
        this.events = events;
        this.platform = platform;
    }

    public static boolean allowCommand(ServerPlayer nativePlayer, String command) {
        var bridge = active;
        if (bridge == null) return true;

        return bridge.command(nativePlayer, command);
    }

    private boolean command(ServerPlayer nativePlayer, String command) {
        var uuid = nativePlayer.getUUID();
        if (commandRedispatch.remove(uuid)) return true;

        var wrapped = wrap(nativePlayer);
        if (wrapped.isEmpty()) return true;

        var normalized = command.startsWith("/") ? command : "/" + command;
        var event = new PlayerCommandPreprocessEvent(wrapped.get(), normalized);
        events.fire(event);
        if (event.cancelled()) return false;

        if (event.command().equals(normalized)) return true;

        commandRedispatch.add(uuid);
        try {
            platform.dispatchPlayerCommand(wrapped.get(), stripSlash(event.command()));
            return false;
        } finally {
            commandRedispatch.remove(uuid);
        }
    }

    private Optional<CellPlayer> wrap(ServerPlayer player) {
        return platform.player(player);
    }

    private static String stripSlash(String command) {
        return command.startsWith("/") ? command.substring(1) : command;
    }

    public static boolean allowPickup(ServerPlayer nativePlayer, ItemEntity entity) {
        var bridge = active;
        if (bridge == null) return true;

        return bridge.pickup(nativePlayer, entity);
    }

    private boolean pickup(ServerPlayer nativePlayer, ItemEntity entity) {
        var stack = entity.getItem();
        if (stack.isEmpty()) return true;

        return wrap(nativePlayer)
                .map(player -> events.fireCancellable(new PlayerPickupEvent(
                        player,
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                        stack.getCount()
                )))
                .orElse(true);
    }

    public static boolean allowFish(ServerPlayer nativePlayer, ItemStack usedItem) {
        var bridge = active;
        if (bridge == null) return true;

        return bridge.wrap(nativePlayer)
                .map(player -> bridge.events.fireCancellable(new PlayerFishEvent(
                        player,
                        PlayerFishEvent.Action.REEL_IN,
                        BuiltInRegistries.ITEM.getKey(usedItem.getItem()).toString()
                )))
                .orElse(true);
    }

    public static boolean allowSignUpdate(
            ServerPlayer nativePlayer,
            BlockPos position,
            boolean front,
            String[] lines
    ) {
        var bridge = active;
        if (bridge == null) return true;

        return bridge.signUpdate(nativePlayer, position, front, lines);
    }

    private boolean signUpdate(
            ServerPlayer nativePlayer,
            BlockPos position,
            boolean front,
            String[] lines
    ) {
        var wrapped = wrap(nativePlayer);
        if (wrapped.isEmpty()) return true;

        var blockEntity = nativePlayer.level().getBlockEntity(position);
        if (!(blockEntity instanceof SignBlockEntity sign)) return true;

        var previous = signLines(sign, front);
        var next = List.copyOf(Arrays.asList(lines));
        var location = location(nativePlayer, position);
        if (previous.stream().allMatch(String::isBlank)) {
            return events.fireCancellable(new SignCreateEvent(
                    wrapped.get(),
                    location,
                    front,
                    next
            ));
        }
        return events.fireCancellable(new SignEditEvent(
                wrapped.get(),
                location,
                front,
                previous,
                next
        ));
    }

    private List<String> signLines(SignBlockEntity sign, boolean front) {
        return Arrays.stream((front ? sign.getFrontText() : sign.getBackText()).getMessages(false))
                .map(Component::getString)
                .toList();
    }

    private CellLocation location(ServerPlayer player, BlockPos position) {
        return new CellLocation(
                player.level().dimension().identifier().toString(),
                position.getX() + 0.5D,
                position.getY(),
                position.getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
    }

    public void register() {
        active = this;

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, _) ->
                wrap(sender)
                        .map(player ->
                                events.fireCancellable(new PlayerChatEvent(
                                        player,
                                        message.decoratedContent().getString()
                                ))
                        )
                        .orElse(true)
        );

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (!(entity instanceof ServerPlayer nativePlayer)) return true;

            return wrap(nativePlayer)
                    .map(player ->
                            events.fireCancellable(new PlayerDamageEvent(
                                    player,
                                    source.toString(),
                                    Optional.ofNullable(source.getEntity()).map(Entity::getUUID),
                                    amount
                            ))
                    )
                    .orElse(true);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof ServerPlayer nativePlayer)) return;

            wrap(nativePlayer).ifPresent(player ->
                    events.fire(new PlayerDeathEvent(
                            player,
                            platform.location(player),
                            source.toString()
                    ))
            );
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((_, nativePlayer, alive) ->
                wrap(nativePlayer).ifPresent(player -> {
                    var event = new PlayerRespawnEvent(player, platform.location(player), alive);
                    events.fire(event);
                    if (!sameLocation(event.location(), platform.location(player))) {
                        platform.teleport(player, event.location());
                    }
                })
        );

        EntitySleepEvents.START_SLEEPING.register((entity, position) -> {
            if (!(entity instanceof ServerPlayer nativePlayer)) return;
            wrap(nativePlayer).ifPresent(player -> {
                var event = new PlayerSleepEvent(
                        player,
                        location(nativePlayer, position),
                        PlayerSleepEvent.Action.START
                );
                events.fire(event);
                if (event.cancelled()) nativePlayer.stopSleeping();
            });
        });

        EntitySleepEvents.STOP_SLEEPING.register((entity, position) -> {
            if (!(entity instanceof ServerPlayer nativePlayer)) return;
            wrap(nativePlayer).ifPresent(player ->
                    events.fire(new PlayerSleepEvent(
                            player,
                            location(nativePlayer, position),
                            PlayerSleepEvent.Action.STOP
                    ))
            );
        });

        AttackEntityCallback.EVENT.register((nativePlayer, level, _, target, _) -> {
            if (level.isClientSide() || !(nativePlayer instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            return wrap(serverPlayer)
                    .map(player -> {
                        var targetPlayer = target instanceof ServerPlayer
                                ? Optional.of(target.getUUID())
                                : Optional.<UUID>empty();
                        var type = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString();
                        var allowed = events.fireCancellable(new PlayerAttackEvent(player, targetPlayer, type));

                        return allowed
                                ? InteractionResult.PASS
                                : InteractionResult.FAIL;
                    })
                    .orElse(InteractionResult.PASS);
        });

        UseItemCallback.EVENT.register((nativePlayer, level, hand) -> {
            if (level.isClientSide() || !(nativePlayer instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            var stack = serverPlayer.getItemInHand(hand);
            if (!BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals("minecraft:fishing_rod")
                    || serverPlayer.fishing != null) {
                return InteractionResult.PASS;
            }

            return wrap(serverPlayer)
                    .map(player ->
                            events.fireCancellable(new PlayerFishEvent(
                                    player,
                                    PlayerFishEvent.Action.CAST,
                                    "minecraft:fishing_hook"
                            ))
                                    ? InteractionResult.PASS
                                    : InteractionResult.FAIL
                    )
                    .orElse(InteractionResult.PASS);
        });

        PlayerBlockBreakEvents.BEFORE.register((_, nativePlayer, position, _, blockEntity) -> {
            if (!(nativePlayer instanceof ServerPlayer serverPlayer)
                    || !(blockEntity instanceof SignBlockEntity sign)) {
                return true;
            }

            return wrap(serverPlayer)
                    .map(player ->
                            events.fireCancellable(new SignBreakEvent(
                                    player,
                                    location(serverPlayer, position),
                                    signLines(sign, true),
                                    signLines(sign, false)
                            ))
                    )
                    .orElse(true);
        });
    }

    private static boolean sameLocation(CellLocation first, CellLocation second) {
        return first.world.equals(second.world)
                && Double.compare(first.x, second.x) == 0
                && Double.compare(first.y, second.y) == 0
                && Double.compare(first.z, second.z) == 0
                && Float.compare(first.yaw, second.yaw) == 0
                && Float.compare(first.pitch, second.pitch) == 0;
    }

    public void playerJoined(ServerPlayer nativePlayer) {
        wrap(nativePlayer).ifPresent(player ->
                snapshots.put(
                        player.uuid(),
                        snapshot(nativePlayer, player)
                )
        );
    }

    private PlayerSnapshot snapshot(ServerPlayer nativePlayer, CellPlayer player) {
        return new PlayerSnapshot(
                platform.location(player),
                nativePlayer.gameMode.getGameModeForPlayer().getName(),
                nativePlayer.containerMenu.containerId,
                nativePlayer.containerMenu.getClass().getSimpleName()
        );
    }

    public void playerDisconnected(ServerPlayer nativePlayer) {
        snapshots.remove(nativePlayer.getUUID());
    }

    public void tick(MinecraftServer server) {
        var online = new HashSet<UUID>();
        server.getPlayerList().getPlayers().forEach(nativePlayer -> {
            var wrapped = wrap(nativePlayer);
            if (wrapped.isEmpty()) return;

            var player = wrapped.get();
            online.add(player.uuid());

            var current = snapshot(nativePlayer, player);
            var previous = snapshots.put(player.uuid(), current);
            if (previous == null) return;

            processMovement(player, previous, current);
            processWorldChange(player, previous, current);
            processGameMode(player, previous, current);
            processInventoryClose(player, previous, current);
        });
        snapshots.keySet().removeIf(uuid -> !online.contains(uuid));
    }

    private void processMovement(
            CellPlayer player,
            PlayerSnapshot previous,
            PlayerSnapshot current
    ) {
        if (sameLocation(previous.location, current.location)) return;

        var event = new PlayerMoveEvent(player, previous.location, current.location);
        events.fire(event);

        if (event.cancelled()) {
            var destination = sameLocation(event.to(), current.location)
                    ? previous.location
                    : event.to();
            platform.teleport(player, destination);
            snapshots.put(player.uuid(), current.withLocation(destination));
        } else if (!sameLocation(event.to(), current.location)) {
            platform.teleport(player, event.to());
            snapshots.put(player.uuid(), current.withLocation(event.to()));
        }
    }

    private void processWorldChange(
            CellPlayer player,
            PlayerSnapshot previous,
            PlayerSnapshot current
    ) {
        if (previous.location.world.equals(current.location.world)) return;

        events.fire(new PlayerWorldChangeEvent(
                player,
                previous.location.world,
                current.location.world
        ));
    }

    private void processGameMode(
            CellPlayer player,
            PlayerSnapshot previous,
            PlayerSnapshot current
    ) {
        if (previous.gameMode.equals(current.gameMode)) return;

        var event = new PlayerGameModeChangeEvent(player, previous.gameMode, current.gameMode);
        events.fire(event);
        if (event.cancelled()) {
            platform.dispatchPlayerCommand(player, "minecraft:gamemode " + previous.gameMode + " @s");
            snapshots.put(player.uuid(), current.withGameMode(previous.gameMode));
        }
    }

    private void processInventoryClose(
            CellPlayer player,
            PlayerSnapshot previous,
            PlayerSnapshot current
    ) {
        if (previous.containerId == 0 || previous.containerId == current.containerId) return;

        events.fire(new InventoryCloseEvent(player, previous.inventoryType));
    }

    private record PlayerSnapshot(
            CellLocation location,
            String gameMode,
            int containerId,
            String inventoryType
    ) {

        private PlayerSnapshot withLocation(CellLocation location) {
            return new PlayerSnapshot(location, gameMode, containerId, inventoryType);
        }

        private PlayerSnapshot withGameMode(String gameMode) {
            return new PlayerSnapshot(location, gameMode, containerId, inventoryType);
        }

    }

}
