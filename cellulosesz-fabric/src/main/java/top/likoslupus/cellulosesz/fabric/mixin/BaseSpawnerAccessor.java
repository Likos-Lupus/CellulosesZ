package top.likoslupus.cellulosesz.fabric.mixin;

import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BaseSpawner.class)
public interface BaseSpawnerAccessor {

    @Accessor("spawnDelay")
    void cellulosesz$setSpawnDelay(int ticks);

}
