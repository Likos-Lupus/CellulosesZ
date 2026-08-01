package top.likoslupus.cellulosesz.fabric;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.world.EntityRemovalPlatformService;
import top.likoslupus.cellulosesz.api.world.EntityRemovalRequest;
import top.likoslupus.cellulosesz.api.world.EntityRemovalResult;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public final class FabricEntityRemovalOperations implements EntityRemovalPlatformService {

    private final FabricServerAccess access;

    public FabricEntityRemovalOperations(FabricServerAccess access) {
        this.access = requireNonNull(access, "access");
    }

    @Override
    public PlatformResult<EntityRemovalResult> remove(EntityRemovalRequest request) {
        requireNonNull(request, "request");
        if (!access.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        if (request.origin().isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_SOURCE,
                    "An online player origin is required"
            );
        }

        var origin = access.player(request.origin().orElseThrow());
        var filter = filter(request);
        if (filter.isEmpty()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Entity selector is invalid"
            );
        }

        var maximumDistance = (double) request.radius() * request.radius();
        var targets = StreamSupport.stream(
                        origin.level().getAllEntities().spliterator(),
                        false
                )
                .filter(entity -> !(entity instanceof ServerPlayer))
                .filter(entity -> entity.distanceToSqr(origin) <= maximumDistance)
                .filter(filter.orElseThrow())
                .toList();

        var removed = 0;
        for (var entity : targets) {
            entity.discard();
            if (entity.isRemoved()) {
                removed++;
            }
        }

        var result = new EntityRemovalResult(
                targets.size(),
                removed,
                targets.size() - removed
        );

        return removed == targets.size()
                ? PlatformResult.success(result)
                : PlatformResult.partial(result, "Some entities could not be removed");
    }

    private static Optional<Predicate<Entity>> filter(EntityRemovalRequest request) {
        return switch (request.selector().kind()) {
            case ALL -> Optional.of(_ -> true);
            case ANIMALS -> Optional.of(entity -> entity instanceof Animal);
            case MONSTERS -> Optional.of(entity -> entity instanceof Enemy
                    || entity instanceof Mob mob && mob.isAggressive()
            );
            case ITEMS -> Optional.of(entity -> entity instanceof ItemEntity);
            case PROJECTILES -> Optional.of(entity -> entity instanceof Projectile);
            case BOATS -> Optional.of(entity -> entity instanceof AbstractBoat);
            case MINECARTS -> Optional.of(entity -> entity instanceof AbstractMinecart);
            case ENTITY_TYPE -> entityType(request.selector().entityId().orElseThrow())
                    .map(type -> entity -> entity.getType() == type);
        };
    }

    private static Optional<EntityType<?>> entityType(String id) {
        var normalized = id.indexOf(':') < 0
                ? "minecraft:" + id
                : id;
        var location = ResourceLocation.tryParse(normalized);

        if (location == null) {
            return Optional.empty();
        }

        return BuiltInRegistries.ENTITY_TYPE.getOptional(location);
    }

}
