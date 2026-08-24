package top.likoslupus.cellulosesz.common.entity;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

import java.util.Set;

public interface EntityPlatformService {

    Set<String> livingEntityIds();

    boolean validLivingEntity(String entityId);

    PlatformResult<SpawnMobResult> spawnMob(SpawnMobRequest request);

    PlatformResult<ProjectileLaunchResult> launchProjectile(ProjectileRequest request);

    PlatformResult<TntBurstResult> spawnTnt(TntBurstRequest request);

    PlatformResult<TemporaryMobResult> launchTemporaryMob(TemporaryMobRequest request);

    void tick();

    void clearTrackedEntities();

}
