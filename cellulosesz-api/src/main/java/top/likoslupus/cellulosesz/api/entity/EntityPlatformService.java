package top.likoslupus.cellulosesz.api.entity;

import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface EntityPlatformService {

    boolean validLivingEntity(String entityId);

    PlatformResult<SpawnMobResult> spawnMob(SpawnMobRequest request);

    PlatformResult<ProjectileLaunchResult> launchProjectile(ProjectileRequest request);

    PlatformResult<TntBurstResult> spawnTnt(TntBurstRequest request);

    PlatformResult<TemporaryMobResult> launchTemporaryMob(TemporaryMobRequest request);

    void tick();

    void clearTrackedEntities();

}
