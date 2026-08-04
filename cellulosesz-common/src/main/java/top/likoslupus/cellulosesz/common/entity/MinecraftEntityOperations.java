package top.likoslupus.cellulosesz.common.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import top.likoslupus.cellulosesz.api.entity.*;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayerUnavailableException;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.common.world.MinecraftWorlds;

import java.util.*;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public final class MinecraftEntityOperations implements EntityPlatformService {

    private static final Set<EntityType<?>> DENIED_TYPES = Set.of(
            EntityType.PLAYER,
            EntityType.ENDER_DRAGON,
            EntityType.WITHER,
            EntityType.COMMAND_BLOCK_MINECART,
            EntityType.MARKER,
            EntityType.INTERACTION,
            EntityType.BLOCK_DISPLAY,
            EntityType.ITEM_DISPLAY,
            EntityType.TEXT_DISPLAY
    );

    private final MinecraftServerHandle server;
    private final Map<UUID, TrackedEntity> tracked = new HashMap<>();
    private long ticks;

    public MinecraftEntityOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public Set<String> livingEntityIds() {
        var level = server.requireRunning().overworld();
        var result = new TreeSet<String>();

        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            if (DENIED_TYPES.contains(type) || !type.canSummon()) {
                return;
            }

            var entity = type.create(level, EntitySpawnReason.COMMAND);
            if (entity instanceof LivingEntity) {
                result.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            }

            if (entity != null) {
                entity.discard();
            }
        });

        return Set.copyOf(result);
    }

    @Override
    public boolean validLivingEntity(String entityId) {
        var location = Identifier.tryParse(normalize(entityId));
        if (location == null) {
            return false;
        }

        var type = BuiltInRegistries.ENTITY_TYPE
                .getOptional(location)
                .orElse(null);
        if (type == null
                || DENIED_TYPES.contains(type)
                || !type.canSummon()
        ) {
            return false;
        }

        var level = server.requireRunning().overworld();
        var entity = type.create(level, EntitySpawnReason.COMMAND);

        if (entity == null) {
            return false;
        }

        var result = entity instanceof LivingEntity;
        entity.discard();
        return result;
    }

    @Override
    public PlatformResult<SpawnMobResult> spawnMob(SpawnMobRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var location = Identifier.tryParse(normalize(request.entityId()));
            if (location == null) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Entity id is invalid"
                );
            }

            var type = BuiltInRegistries.ENTITY_TYPE
                    .getOptional(location)
                    .orElse(null);
            if (type == null
                    || DENIED_TYPES.contains(type)
                    || !type.canSummon()
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INVALID_ARGUMENT,
                        "Entity type is not allowed"
                );
            }

            var anchor = MinecraftPlayers.requireOnline(server, request.anchor());
            var level = anchor.level();
            var base = anchor.blockPosition().relative(anchor.getDirection(), 2);
            if (!level.isLoaded(base)
                    || !level.getWorldBorder().isWithinBounds(base)
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Spawn position is unavailable"
                );
            }

            var spawned = 0;
            for (var index = 0; index < request.amount(); index++) {
                var entity = type.create(level, EntitySpawnReason.COMMAND);
                if (!(entity instanceof LivingEntity)) {
                    if (entity != null) {
                        entity.discard();
                    }
                    break;
                }

                entity.snapTo(
                        base.getX() + 0.5D, base.getY(), base.getZ() + 0.5D,
                        anchor.getYRot(), 0.0F
                );

                if (entity instanceof Mob mob) {
                    mob.finalizeSpawn(
                            level,
                            level.getCurrentDifficultyAt(base),
                            EntitySpawnReason.COMMAND,
                            null
                    );
                }

                if (!level.addFreshEntity(entity)) {
                    break;
                }
                spawned++;
            }

            var result = new SpawnMobResult(
                    location.toString(),
                    request.amount(),
                    spawned
            );

            if (spawned == request.amount()) {
                return PlatformResult.success(result);
            }

            if (spawned > 0) {
                return PlatformResult.partial(result, "Some entities could not be spawned");
            }

            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    "No entities were spawned"
            );
        });
    }

    @Override
    public PlatformResult<ProjectileLaunchResult> launchProjectile(ProjectileRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var shooter = MinecraftPlayers.requireOnline(server, request.shooter());
            var level = shooter.level();
            var entity = createProjectile(level, request.type());

            if (!(entity instanceof Projectile projectile)) {
                entity.discard();
                return PlatformResult.failure(
                        PlatformOperationStatus.UNSUPPORTED,
                        "Projectile type is unavailable"
                );
            }

            var direction = shooter.getLookAngle();
            if (!validDirection(direction)) {
                entity.discard();
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "View direction is invalid"
                );
            }

            entity.setPos(shooter.getX(), shooter.getEyeY() - 0.1D, shooter.getZ());
            projectile.setOwner(shooter);
            projectile.shoot(
                    direction.x, direction.y, direction.z,
                    (float) request.speed(),
                    0.0F
            );

            if (!level.addFreshEntity(projectile)) {
                projectile.discard();
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Projectile could not be spawned"
                );
            }

            tracked.put(
                    projectile.getUUID(),
                    new TrackedEntity(
                            level.dimension().identifier().toString(),
                            ticks + request.lifetimeTicks(),
                            0.0D,
                            false,
                            false
                    )
            );

            return PlatformResult.success(new ProjectileLaunchResult(
                    projectile.getUUID(),
                    request.type()
            ));
        });
    }

    @Override
    public PlatformResult<TntBurstResult> spawnTnt(TntBurstRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var level = MinecraftWorlds.findLoaded(
                    server.requireRunning(),
                    request.center().world()
            );
            if (level.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.TARGET_NOT_FOUND,
                        "World was not found"
                );
            }

            var targetLevel = level.orElseThrow();
            var spawned = 0;
            for (var index = 0; index < request.amount(); index++) {
                var angle = (Math.PI * 2.0D * index) / request.amount();
                var radius = request.amount() == 1
                        ? 0.0D
                        : request.spread() * ((index % 5) / 4.0D);
                var x = request.center().x() + Math.cos(angle) * radius;
                var y = request.center().y() + request.height();
                var z = request.center().z() + Math.sin(angle) * radius;
                var position = BlockPos.containing(x, y, z);

                if (!targetLevel.isLoaded(position)
                        || !targetLevel
                        .getWorldBorder()
                        .isWithinBounds(position)
                ) {
                    continue;
                }

                var tnt = new PrimedTnt(targetLevel, x, y, z, null);
                tnt.setFuse(request.fuseTicks());

                if (!targetLevel.addFreshEntity(tnt)) {
                    continue;
                }

                tracked.put(
                        tnt.getUUID(),
                        new TrackedEntity(
                                targetLevel.dimension().identifier().toString(),
                                ticks + request.fuseTicks() + 2L,
                                request.explosionPower(),
                                request.blockDamage(),
                                true
                        )
                );
                spawned++;
            }

            var result = new TntBurstResult(request.amount(), spawned);
            if (spawned == request.amount()) {
                return PlatformResult.success(result);
            }

            if (spawned > 0) {
                return PlatformResult.partial(result, "Some TNT entities could not be spawned");
            }

            return PlatformResult.failure(
                    PlatformOperationStatus.STATE_NOT_ALLOWED,
                    "No TNT entities were spawned"
            );
        });
    }

    @Override
    public PlatformResult<TemporaryMobResult> launchTemporaryMob(TemporaryMobRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var shooter = MinecraftPlayers.requireOnline(server, request.shooter());
            var level = shooter.level();
            var entity = (Entity) switch (request.type()) {
                case BEE -> EntityType.BEE.create(level, EntitySpawnReason.COMMAND);
                case CAT -> EntityType.CAT.create(level, EntitySpawnReason.COMMAND);
            };

            if (!(entity instanceof LivingEntity living)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.INTERNAL_ERROR,
                        "Temporary entity creation failed"
                );
            }

            ((Mob) living).setNoAi(true);

            var direction = shooter.getLookAngle();
            if (!validDirection(direction)) {
                living.discard();
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "View direction is invalid"
                );
            }

            living.setPos(shooter.getX(), shooter.getEyeY(), shooter.getZ());
            living.setDeltaMovement(direction.scale(request.speed()));
            if (!level.addFreshEntity(living)) {
                living.discard();
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Temporary entity could not be spawned"
                );
            }

            tracked.put(
                    living.getUUID(),
                    new TrackedEntity(
                            level.dimension().identifier().toString(),
                            ticks + request.lifetimeTicks(),
                            request.explosionPower(),
                            request.blockDamage(),
                            false
                    )
            );

            return PlatformResult.success(new TemporaryMobResult(living.getUUID(), request.type()));
        });
    }

    @Override
    public void tick() {
        if (!server.serverThread()) {
            return;
        }

        ticks++;
        var removals = new ArrayList<UUID>();
        tracked.forEach((uuid, state) -> {
            var level = MinecraftWorlds
                    .findLoaded(server.requireRunning(), state.worldId())
                    .orElse(null);
            if (level == null) {
                removals.add(uuid);
                return;
            }

            var entity = level.getEntity(uuid);
            if (entity == null || entity.isRemoved()) {
                removals.add(uuid);
                return;
            }

            var detonate = state.customTnt()
                    && entity instanceof PrimedTnt tnt
                    && tnt.getFuse() <= 1;
            var impact = !state.customTnt() && (
                    entity.onGround()
                            || entity.horizontalCollision
                            || entity.verticalCollision
            );

            if (ticks >= state.expiresAt()
                    || detonate
                    || impact
            ) {
                var position = entity.position();
                entity.discard();
                explode(level, position, state);
                removals.add(uuid);
            }
        });

        removals.forEach(tracked::remove);
    }

    @Override
    public void clearTrackedEntities() {
        if (!server.serverThread()) {
            return;
        }

        tracked.forEach((uuid, state) -> MinecraftWorlds
                .findLoaded(server.requireRunning(), state.worldId())
                .map(level -> level.getEntity(uuid))
                .ifPresent(Entity::discard));
        tracked.clear();
    }

    private void explode(
            ServerLevel level,
            Vec3 position,
            TrackedEntity state
    ) {
        if (state.explosionPower() <= 0.0D) {
            return;
        }

        level.explode(
                null,
                position.x,
                position.y,
                position.z,
                (float) state.explosionPower(),
                state.blockDamage()
                        ? Level.ExplosionInteraction.TNT
                        : Level.ExplosionInteraction.NONE
        );
    }

    private static Entity createProjectile(ServerLevel level, ProjectileType type) {
        var entityType = switch (type) {
            case FIREBALL, LARGE -> EntityType.FIREBALL;
            case SMALL -> EntityType.SMALL_FIREBALL;
            case ARROW -> EntityType.ARROW;
            case SKULL -> EntityType.WITHER_SKULL;
            case EGG -> EntityType.EGG;
            case SNOWBALL -> EntityType.SNOWBALL;
            case EXPERIENCE_BOTTLE -> EntityType.EXPERIENCE_BOTTLE;
            case DRAGON -> EntityType.DRAGON_FIREBALL;
            case SPLASH_POTION -> EntityType.SPLASH_POTION;
            case LINGERING_POTION -> EntityType.LINGERING_POTION;
            case TRIDENT -> EntityType.TRIDENT;
        };

        var entity = entityType.create(level, EntitySpawnReason.COMMAND);
        if (entity instanceof AbstractThrownPotion potion) {
            potion.setItem(new ItemStack(
                    type == ProjectileType.LINGERING_POTION
                            ? Items.LINGERING_POTION
                            : Items.SPLASH_POTION
            ));
        }

        return entity;
    }

    private static boolean validDirection(Vec3 direction) {
        return Double.isFinite(direction.x)
                && Double.isFinite(direction.y)
                && Double.isFinite(direction.z)
                && direction.lengthSqr() > 1.0E-12D;
    }

    private <T> PlatformResult<T> onServerThread(Supplier<PlatformResult<T>> operation) {
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return operation.get();
        } catch (MinecraftPlayerUnavailableException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    failure.getMessage()
            );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private static String normalize(String value) {
        var id = value.strip().toLowerCase(Locale.ROOT);
        return id.contains(":")
                ? id
                : "minecraft:" + id;
    }

    private record TrackedEntity(
            String worldId,
            long expiresAt,
            double explosionPower,
            boolean blockDamage,
            boolean customTnt
    ) {

    }

}
