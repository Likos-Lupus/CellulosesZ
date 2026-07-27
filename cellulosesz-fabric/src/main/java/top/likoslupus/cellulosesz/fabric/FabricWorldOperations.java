package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.world.*;
import top.likoslupus.cellulosesz.fabric.mixin.BaseSpawnerAccessor;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.OptionalInt;

import static java.util.Objects.requireNonNull;

public final class FabricWorldOperations implements WorldPlatformService {

    private static final Map<TreeType, String> TREE_FEATURES = Map.ofEntries(
            Map.entry(TreeType.OAK, "minecraft:oak"),
            Map.entry(TreeType.BIRCH, "minecraft:birch"),
            Map.entry(TreeType.SPRUCE, "minecraft:spruce"),
            Map.entry(TreeType.RED_MUSHROOM, "minecraft:huge_red_mushroom"),
            Map.entry(TreeType.BROWN_MUSHROOM, "minecraft:huge_brown_mushroom"),
            Map.entry(TreeType.JUNGLE, "minecraft:mega_jungle_tree"),
            Map.entry(TreeType.JUNGLE_BUSH, "minecraft:jungle_bush"),
            Map.entry(TreeType.SWAMP, "minecraft:swamp_oak"),
            Map.entry(TreeType.LARGE_OAK, "minecraft:fancy_oak"),
            Map.entry(TreeType.LARGE_SPRUCE, "minecraft:mega_spruce"),
            Map.entry(TreeType.LARGE_JUNGLE, "minecraft:mega_jungle_tree"),
            Map.entry(TreeType.DARK_OAK, "minecraft:dark_oak")
    );

    private final FabricPlatformService platform;

    public FabricWorldOperations(FabricPlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public PlatformResult<ServerDiagnosticsSnapshot> diagnostics() {
        return onServerThread(() -> {
            var server = platform.requireServer();
            var runtime = Runtime.getRuntime();
            var allocated = runtime.totalMemory();
            var free = runtime.freeMemory();
            var maximum = runtime.maxMemory();
            var used = allocated - free;
            var averageNanos = server.getAverageTickTimeNanos();
            var averageMillis = averageNanos <= 0L
                    ? OptionalDouble.empty()
                    : OptionalDouble.of(averageNanos / 1_000_000.0D);
            var tps = averageMillis.isEmpty() || averageMillis.orElseThrow() == 0.0D
                    ? 20.0D
                    : Math.min(20.0D, 1000.0D / averageMillis.orElseThrow());
            var worlds = new ArrayList<WorldDiagnostics>();
            server.getAllLevels().forEach(level -> {
                var entities = 0;
                for (var ignored : level.getAllEntities()) entities++;
                worlds.add(new WorldDiagnostics(
                        level.dimension().identifier().toString(),
                        level.getChunkSource().getLoadedChunksCount(),
                        entities,
                        OptionalInt.empty()
                ));
            });
            return PlatformResult.success(new ServerDiagnosticsSnapshot(
                    Math.max(0L, ManagementFactory.getRuntimeMXBean().getUptime()),
                    used,
                    allocated,
                    maximum,
                    free + Math.max(0L, maximum - allocated),
                    tps,
                    averageMillis,
                    worlds
            ));
        });
    }

    @Override
    public PlatformResult<BlockBreakResult> breakTarget(
            CellPlayer player, int maximumDistance, boolean allowUnbreakable) {
        if (maximumDistance < 1)
            return PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "Distance must be positive");
        return onServerThread(() -> {
            var nativePlayer = platform.nativePlayer(player);
            var hit = nativePlayer.pick(maximumDistance, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "No block is targeted");
            }
            var level = nativePlayer.level();
            var position = blockHit.getBlockPos();
            var state = level.getBlockState(position);
            if (state.isAir()) return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "Target is air");
            if (state.getDestroySpeed(level, position) < 0.0F && !allowUnbreakable) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Target is unbreakable");
            }
            var unbreakable = state.getDestroySpeed(level, position) < 0.0F;
            if (unbreakable) {
                var blockEntity = level.getBlockEntity(position);
                if (!PlayerBlockBreakEvents.BEFORE.invoker()
                        .beforeBlockBreak(level, nativePlayer, position, state, blockEntity)) {
                    return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Block break was cancelled");
                }
            }
            var id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            // The normal game-mode path invokes Fabric's block-break hooks itself. Only the explicit
            // unbreakable bypass performs the event check above before using the level mutation API.
            var broken = unbreakable
                    ? level.destroyBlock(position, true, nativePlayer)
                    : nativePlayer.gameMode.destroyBlock(position);
            return broken
                    ? PlatformResult.success(new BlockBreakResult(id, true))
                    : PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Block could not be broken");
        });
    }

    @Override
    public PlatformResult<SignTarget> targetSign(CellPlayer player, int maximumDistance) {
        if (maximumDistance < 1)
            return PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "Distance must be positive");
        return onServerThread(() -> {
            var nativePlayer = platform.nativePlayer(player);
            var hit = nativePlayer.pick(maximumDistance, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "No sign is targeted");
            }
            var blockEntity = nativePlayer.level().getBlockEntity(blockHit.getBlockPos());
            if (!(blockEntity instanceof SignBlockEntity sign)) {
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "Target is not a sign");
            }
            var front = sign.isFacingFrontText(nativePlayer);
            var messages = (front ? sign.getFrontText() : sign.getBackText()).getMessages(false);
            var lines = java.util.Arrays.stream(messages).map(component -> component.getString()).toList();
            return PlatformResult.success(new SignTarget(
                    location(nativePlayer.level(), blockHit.getBlockPos()),
                    front,
                    lines,
                    sign.isWaxed()
            ));
        });
    }

    @Override
    public PlatformResult<SignTarget> replaceSignText(
            CellPlayer player, SignTextMutation mutation, boolean allowWaxed) {
        requireNonNull(mutation, "mutation");
        return onServerThread(() -> {
            var target = mutation.target();
            if (target.waxed() && !allowWaxed) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Sign is waxed");
            }
            var changed = platform.replaceSignText(
                    player,
                    target.location(),
                    target.front(),
                    target.lines(),
                    mutation.replacementLines()
            );
            if (!changed) return PlatformResult.failure(PlatformOperationStatus.CONFLICT, "Sign changed before commit");
            return PlatformResult.success(new SignTarget(
                    target.location(), target.front(), mutation.replacementLines(), target.waxed()
            ));
        });
    }

    @Override
    public PlatformResult<SpawnerResult> configureSpawner(
            CellPlayer player, int maximumDistance, SpawnerRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var location = ResourceLocation.tryParse(normalize(request.entityId()));
            if (location == null)
                return PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "Entity id is invalid");
            var type = BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
            if (type == null || !type.canSummon() || type == EntityType.PLAYER) {
                return PlatformResult.failure(PlatformOperationStatus.INVALID_ARGUMENT, "Entity cannot be spawned");
            }
            var nativePlayer = platform.nativePlayer(player);
            var hit = nativePlayer.pick(maximumDistance, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit)) {
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "No spawner is targeted");
            }
            var blockEntity = nativePlayer.level().getBlockEntity(blockHit.getBlockPos());
            if (!(blockEntity instanceof SpawnerBlockEntity spawner)) {
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "Target is not a spawner");
            }
            spawner.getSpawner().setEntityId(type, nativePlayer.level().getRandom());
            ((BaseSpawnerAccessor) spawner.getSpawner()).cellulosesz$setSpawnDelay(request.delayTicks());
            spawner.setChanged();
            var state = nativePlayer.level().getBlockState(blockHit.getBlockPos());
            nativePlayer.level().sendBlockUpdated(blockHit.getBlockPos(), state, state, 3);
            return PlatformResult.success(new SpawnerResult(location.toString(), request.delayTicks()));
        });
    }

    @Override
    public PlatformResult<TreeGenerationResult> generateTree(CellPlayer player, int maximumDistance, TreeType type) {
        requireNonNull(type, "type");
        return onServerThread(() -> {
            var nativePlayer = platform.nativePlayer(player);
            var hit = nativePlayer.pick(maximumDistance, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit)) {
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "No generation position is targeted");
            }
            var position = blockHit.getBlockPos().relative(blockHit.getDirection());
            var level = nativePlayer.level();
            if (!level.getWorldBorder().isWithinBounds(position) || !level.hasChunkAt(position)) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Generation position is unavailable");
            }
            if (!level.getBlockState(position).canBeReplaced()) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Generation position is obstructed");
            }
            var id = ResourceLocation.tryParse(TREE_FEATURES.get(type));
            if (id == null)
                return PlatformResult.failure(PlatformOperationStatus.UNSUPPORTED, "Tree type is unsupported");
            var key = ResourceKey.create(Registries.CONFIGURED_FEATURE, id);
            var feature = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).get(key).orElse(null);
            if (feature == null)
                return PlatformResult.failure(PlatformOperationStatus.UNSUPPORTED, "Configured feature is unavailable");
            var placed = feature.value()
                    .place(level, level.getChunkSource().getGenerator(), level.getRandom(), position);
            return placed
                    ? PlatformResult.success(new TreeGenerationResult(type, location(level, position)))
                    : PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Feature placement failed without changing terrain");
        });
    }

    @Override
    public PlatformResult<Void> setThunder(String worldId, ThunderRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var level = platform.serverLevel(worldId);
            if (level.isEmpty())
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "World was not found");
            var target = level.orElseThrow();
            target.setWeatherParameters(
                    request.enabled() ? 0 : request.durationTicks(),
                    request.enabled() ? request.durationTicks() : 0,
                    target.isRaining(),
                    request.enabled()
            );
            return PlatformResult.success();
        });
    }

    @Override
    public PlatformResult<Void> strikeLightning(LightningRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var level = platform.serverLevel(request.location().world);
            if (level.isEmpty())
                return PlatformResult.failure(PlatformOperationStatus.TARGET_NOT_FOUND, "World was not found");
            var targetLevel = level.orElseThrow();
            var bolt = EntityType.LIGHTNING_BOLT.create(targetLevel, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (bolt == null)
                return PlatformResult.failure(PlatformOperationStatus.INTERNAL_ERROR, "Lightning entity creation failed");
            bolt.moveTo(request.location().x, request.location().y, request.location().z);
            bolt.setVisualOnly(request.visualOnly());
            if (!targetLevel.addFreshEntity(bolt)) {
                return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Lightning could not be spawned");
            }
            if (request.additionalDamage() > 0.0D) {
                var box = new AABB(
                        request.location().x - 1.5D, request.location().y - 1.0D, request.location().z - 1.5D,
                        request.location().x + 1.5D, request.location().y + 4.0D, request.location().z + 1.5D
                );
                targetLevel.getEntitiesOfClass(LivingEntity.class, box).forEach(entity ->
                        entity.hurtServer(targetLevel, entity.damageSources()
                                .lightningBolt(), (float) request.additionalDamage())
                );
            }
            return PlatformResult.success();
        });
    }

    private static String normalize(String id) {
        var value = id.strip().toLowerCase(java.util.Locale.ROOT);
        return value.contains(":") ? value : "minecraft:" + value;
    }

    private static CellLocation location(ServerLevel level, BlockPos position) {
        return new CellLocation(
                level.dimension().identifier().toString(),
                position.getX(), position.getY(), position.getZ(), 0.0F, 0.0F
        );
    }

    private <T> PlatformResult<T> onServerThread(java.util.function.Supplier<PlatformResult<T>> operation) {
        if (!platform.requireServer().isSameThread()) {
            return PlatformResult.failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Operation requires the server thread");
        }
        try {
            return operation.get();
        } catch (RuntimeException failure) {
            return PlatformResult.failure(PlatformOperationStatus.INTERNAL_ERROR, failure.getClass().getSimpleName());
        }
    }

}
