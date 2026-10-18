package top.likoslupus.cellulosesz.administration.entitytools

import com.mojang.logging.LogUtils
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.*
import net.minecraft.world.entity.ambient.AmbientCreature
import net.minecraft.world.entity.animal.Animal
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.decoration.painting.Painting
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.monster.Monster
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.arrow.AbstractArrow
import net.minecraft.world.entity.vehicle.boat.Boat
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart
import net.minecraft.world.level.block.entity.SpawnerBlockEntity
import net.minecraft.world.level.storage.TagValueInput

/**
 * Server-thread confined Minecraft work for `/spawner`, `/spawnmob` and `/remove`. The selector
 * parsing is pure ([RemoveSelectorParser]); this class only classifies live entities.
 */
internal class EntityToolsService {

    /** Resolves a registry id (with or without namespace) to an entity type. */
    fun resolveEntityType(raw: String): EntityType<*>? {
        val id = Identifier.tryParse(
            when {
                raw.contains(':') -> raw
                else -> "minecraft:$raw"
            }
        ) ?: return null
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null)
    }

    fun knownEntityTypeIds(): List<String> =
        BuiltInRegistries.ENTITY_TYPE.keySet().map(Identifier::toString)

    /**
     * Spawns [amount] of [type] at the given position, spreading each spawn slightly. Returns the
     * number of entities that were actually added.
     */
    fun spawnMobs(
        level: ServerLevel,
        type: EntityType<*>,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
        amount: Int,
        mountType: EntityType<*>? = null,
    ): Int {
        val random = level.random
        var spawned = 0
        repeat(amount) {
            val entity = try {
                type.create(level, EntitySpawnReason.COMMAND)
            } catch (exception: Exception) {
                LOGGER.warn("failed to create entity type {}", EntityType.getKey(type), exception)
                null
            } ?: return@repeat

            val dx = (random.nextDouble() - 0.5) * 0.6
            val dz = (random.nextDouble() - 0.5) * 0.6
            entity.snapTo(x + dx, y, z + dz, yaw, pitch)
            if (entity is Mob) {
                entity.setPersistenceRequired()
            }
            if (mountType != null) {
                val mount = try {
                    mountType.create(level, EntitySpawnReason.COMMAND)
                } catch (exception: Exception) {
                    LOGGER.warn(
                        "failed to create mount entity type {}",
                        EntityType.getKey(mountType),
                        exception,
                    )
                    null
                }
                if (mount != null) {
                    mount.snapTo(x + dx, y, z + dz, yaw, pitch)
                    if (mount is Mob) {
                        mount.setPersistenceRequired()
                    }
                    if (level.addFreshEntity(mount)) {
                        entity.startRiding(mount)
                    }
                }
            }
            if (level.addFreshEntity(entity)) {
                spawned++
            }
        }
        return spawned
    }

    /**
     * Points the spawner at [pos] at [type] and applies [delayTicks] (0 means Essentials' immediate
     * reset). Returns false when the block is not a spawner.
     */
    fun configureSpawner(
        level: ServerLevel,
        pos: BlockPos,
        type: EntityType<*>,
        delayTicks: Int,
    ): Boolean {
        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity !is SpawnerBlockEntity) {
            return false
        }
        val spawner = blockEntity.spawner
        val delay = delayTicks.coerceIn(0, Short.MAX_VALUE.toInt())
        val tag = CompoundTag()
        tag.putShort("Delay", delay.toShort())
        if (delay > 0) {
            tag.putShort("MinSpawnDelay", delay.toShort())
            tag.putShort("MaxSpawnDelay", delay.toShort())
        }
        spawner.load(
            level,
            pos,
            TagValueInput.create(
                ProblemReporter.DISCARDING,
                level.registryAccess(),
                tag,
            ),
        )
        spawner.setEntityId(type, level, level.random, pos)
        blockEntity.setChanged()
        val state = level.getBlockState(pos)
        level.sendBlockUpdated(
            pos,
            state,
            state,
            3
        )
        return true
    }

    /**
     * Discards every matching entity. When [radius] is non-null only entities within that distance
     * of the centre are removed; players are never removed. Returns the number discarded.
     */
    fun remove(
        level: ServerLevel,
        target: RemoveTarget,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        radius: Double?,
    ): Int {
        val limitSqr = radius?.let { it * it }
        val matches = level.allEntities.filter { entity ->
            entity !is Player && matches(entity, target)
        }
        var removed = 0
        matches.forEach { entity ->
            if (
                limitSqr != null
                && entity.distanceToSqr(
                    centerX,
                    centerY,
                    centerZ
                ) > limitSqr
            ) {
                return@forEach
            }
            entity.discard()
            removed++
        }
        return removed
    }

    private fun matches(entity: Entity, target: RemoveTarget): Boolean =
        when (target) {
            is RemoveTarget.Category ->
                matchesCategory(entity, target.selector)

            is RemoveTarget.EntityType ->
                EntityType.getKey(entity.type).toString() == target.id
        }

    private fun matchesCategory(entity: Entity, selector: EntitySelector): Boolean =
        when (selector) {
            EntitySelector.ALL -> true
            EntitySelector.TAMED -> entity is TamableAnimal && entity.isTame
            EntitySelector.NAMED -> entity.hasCustomName()
            EntitySelector.DROPS -> entity is ItemEntity
            EntitySelector.ARROWS -> entity is AbstractArrow
            EntitySelector.BOATS -> entity is Boat
            EntitySelector.MINECARTS -> entity is AbstractMinecart
            EntitySelector.XP -> entity is ExperienceOrb
            EntitySelector.PAINTINGS -> entity is Painting
            EntitySelector.ITEM_FRAMES -> entity is ItemFrame
            EntitySelector.ENDER_CRYSTALS -> entity is EndCrystal
            EntitySelector.MONSTERS -> entity is Monster
            EntitySelector.ANIMALS -> entity is Animal
            EntitySelector.AMBIENT -> entity is AmbientCreature
            EntitySelector.MOBS -> entity is Mob
        }

    private companion object {

        private val LOGGER = LogUtils.getLogger()

    }

}
