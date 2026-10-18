package top.likoslupus.cellulosesz.administration.worldtools

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.protocol.game.ClientboundGameEventPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.clock.ClockNetworkState
import net.minecraft.world.clock.WorldClock
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.PrimedTnt
import net.minecraft.world.entity.projectile.FireworkRocketEntity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.Fireworks
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature
import kotlin.math.floor

/** The three server weather states shared by `/weather` and `/pweather`. */
internal enum class WeatherType {

    CLEAR,
    RAIN,
    THUNDER,

    ;

    companion object {

        fun parse(raw: String): WeatherType? =
            when (raw.lowercase()) {
                "clear", "sunny" -> CLEAR
                "rain" -> RAIN
                "thunder" -> THUNDER
                else -> null
            }

    }

}

/** The projectile kinds accepted by `/fireball`, mirroring Essentials' type table. */
internal enum class FireballType {

    FIREBALL,
    SMALL,
    LARGE,
    DRAGON,
    SKULL,
    ARROW,
    EGG,
    SNOWBALL,
    EXP_BOTTLE,
    SPLASH_POTION,
    LINGERING_POTION,
    TRIDENT,
    WIND_CHARGE,

    ;

    companion object {

        fun parse(raw: String): FireballType? =
            when (raw.lowercase()) {
                "fireball", "big" -> FIREBALL
                "small", "smallfireball" -> SMALL
                "large", "largefireball" -> LARGE
                "dragon", "dragonfireball" -> DRAGON
                "skull", "wither", "witherskull" -> SKULL
                "arrow" -> ARROW
                "egg" -> EGG
                "snowball" -> SNOWBALL
                "expbottle", "experiencebottle" -> EXP_BOTTLE
                "splashpotion", "splash" -> SPLASH_POTION
                "lingeringpotion", "lingering" -> LINGERING_POTION
                "trident" -> TRIDENT
                "windcharge" -> WIND_CHARGE
                else -> null
            }

    }

}

/** A snapshot of point-in-time server resource usage for `/gc`. */
internal data class ServerStats(
    val uptimeTicks: Long,
    val tps: Double,
    val averageTickMillis: Double,
    val usedMemoryBytes: Long,
    val totalMemoryBytes: Long,
    val maxMemoryBytes: Long,
    val worlds: List<WorldStats>,
)

internal data class WorldStats(
    val dimension: String,
    val players: Int,
    val chunks: Int,
    val entities: Int,
    val blockEntities: Int,
)

/**
 * Minecraft-side implementation of `/time`, `/weather`, `/thunder`, `/lightning`, `/fireball`,
 * `/firework`, `/nuke`, `/tree`, `/bigtree`, `/break`, `/ice`, `/ext`, `/burn` and `/gc`, plus the
 * per-player `/ptime` and `/pweather` packets. All methods run on the server thread.
 */
internal class WorldToolsService {

    fun distinctClockHolders(levels: Iterable<ServerLevel>): List<Holder<WorldClock>> =
        levels.mapNotNull { it.dimensionType().defaultClock().orElse(null) }
                .distinctBy { it.unwrapKey().orElse(null) }

    fun clockHolder(level: ServerLevel): Holder<WorldClock>? =
        level.dimensionType().defaultClock().orElse(null)

    fun currentTime(level: ServerLevel): Long? =
        clockHolder(level)?.let { level.clockManager().getTotalTicks(it) }

    fun setTime(level: ServerLevel, ticks: Long) {
        val holder = clockHolder(level) ?: return
        level.clockManager().setTotalTicks(holder, ticks)
    }

    fun addTime(level: ServerLevel, ticks: Long) {
        val holder = clockHolder(level) ?: return
        level.clockManager().addTicks(holder, ticks.toInt())
    }

    fun sendPersonalTime(player: ServerPlayer, view: PersonalTimeView) {
        val holder = clockHolder(player.level()) ?: return
        val state = ClockNetworkState(
            view.ticks,
            0f,
            if (view.frozen) 0f else 1f,
        )
        player.connection.send(
            ClientboundSetTimePacket(
                player.level().gameTime,
                mapOf(holder to state),
            )
        )
    }

    fun sendPersonalWeather(player: ServerPlayer, weather: PersonalWeather) {
        val rain = if (weather == PersonalWeather.CLEAR) 0f else 1f
        val thunder = if (weather == PersonalWeather.THUNDER) 1f else 0f
        player.connection.send(
            ClientboundGameEventPacket(
                ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,
                rain,
            )
        )
        player.connection.send(
            ClientboundGameEventPacket(
                ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,
                thunder,
            )
        )
    }

    fun setWeather(
        level: ServerLevel,
        weather: WeatherType,
        durationTicks: Int?
    ) {
        val data = level.weatherData
        val duration = durationTicks ?: -1
        when (weather) {
            WeatherType.CLEAR -> {
                data.isRaining = false
                data.isThundering = false
                data.clearWeatherTime = duration
                data.rainTime = 0
                data.thunderTime = 0
            }

            WeatherType.RAIN -> {
                data.isRaining = true
                data.isThundering = false
                data.rainTime = duration
                data.clearWeatherTime = 0
                data.thunderTime = 0
            }

            WeatherType.THUNDER -> {
                data.isRaining = true
                data.isThundering = true
                data.rainTime = duration
                data.thunderTime = duration
                data.clearWeatherTime = 0
            }
        }
        level.setRainLevel(if (weather == WeatherType.CLEAR) 0f else 1f)
        level.setThunderLevel(if (weather == WeatherType.THUNDER) 1f else 0f)
        broadcast(level, weather == WeatherType.CLEAR)
    }

    fun setThunder(
        level: ServerLevel,
        enabled: Boolean,
        durationTicks: Int?
    ) {
        val data = level.weatherData
        data.isThundering = enabled
        data.thunderTime = durationTicks ?: -1
        level.setThunderLevel(if (enabled) 1f else 0f)
        broadcast(level, !enabled)
    }

    private fun broadcast(level: ServerLevel, clear: Boolean) {
        val packet = ClientboundGameEventPacket(
            when {
                clear -> ClientboundGameEventPacket.STOP_RAINING
                else -> ClientboundGameEventPacket.START_RAINING
            },
            0f,
        )
        level.players().forEach { player ->
            player.connection.send(packet)
        }
    }

    fun strikeLightning(
        level: ServerLevel,
        x: Double,
        y: Double,
        z: Double,
        visualOnly: Boolean
    ): Boolean {
        val bolt = EntityType.LIGHTNING_BOLT.create(
            level,
            EntitySpawnReason.COMMAND
        ) ?: return false
        bolt.snapTo(x, y, z, 0f, 0f)
        bolt.setVisualOnly(visualOnly)
        return level.addFreshEntity(bolt)
    }

    fun launchFireball(
        player: ServerPlayer,
        type: FireballType,
        speed: Double,
        ride: Boolean,
    ): Boolean {
        val level = player.level()
        val entity = when (type) {
            FireballType.FIREBALL,
            FireballType.LARGE ->
                EntityType.FIREBALL.create(level, EntitySpawnReason.COMMAND)

            FireballType.SMALL ->
                EntityType.SMALL_FIREBALL.create(level, EntitySpawnReason.COMMAND)

            FireballType.DRAGON ->
                EntityType.DRAGON_FIREBALL.create(level, EntitySpawnReason.COMMAND)

            FireballType.SKULL ->
                EntityType.WITHER_SKULL.create(level, EntitySpawnReason.COMMAND)

            FireballType.ARROW ->
                EntityType.ARROW.create(level, EntitySpawnReason.COMMAND)

            FireballType.EGG ->
                EntityType.EGG.create(level, EntitySpawnReason.COMMAND)

            FireballType.SNOWBALL ->
                EntityType.SNOWBALL.create(level, EntitySpawnReason.COMMAND)

            FireballType.EXP_BOTTLE ->
                EntityType.EXPERIENCE_BOTTLE.create(level, EntitySpawnReason.COMMAND)

            FireballType.SPLASH_POTION ->
                EntityType.SPLASH_POTION.create(level, EntitySpawnReason.COMMAND)

            FireballType.LINGERING_POTION ->
                EntityType.LINGERING_POTION.create(level, EntitySpawnReason.COMMAND)

            FireballType.TRIDENT ->
                EntityType.TRIDENT.create(level, EntitySpawnReason.COMMAND)

            FireballType.WIND_CHARGE ->
                EntityType.WIND_CHARGE.create(level, EntitySpawnReason.COMMAND)
        } ?: return false
        entity.snapTo(
            player.x,
            player.eyeY,
            player.z,
            player.yRot,
            player.xRot,
        )
        entity.setOwner(player)
        entity.shootFromRotation(
            player,
            player.xRot,
            player.yRot,
            0f,
            speed.toFloat(),
            0f,
        )
        val added = level.addFreshEntity(entity)
        if (added && ride) {
            player.startRiding(entity)
        }
        return added
    }

    /** Sets the flight power of the held firework rocket; returns false when the hand is empty. */
    fun setFireworkPower(player: ServerPlayer, power: Int): Boolean {
        val stack = player.mainHandItem
        if (stack.item != Items.FIREWORK_ROCKET) {
            return false
        }
        val existing = stack.get(FIREWORKS)
        stack.set(
            FIREWORKS,
            Fireworks(
                power,
                existing?.explosions ?: emptyList()
            ),
        )
        player.inventoryMenu.broadcastChanges()
        return true
    }

    fun clearFirework(player: ServerPlayer): Boolean {
        val stack = player.mainHandItem
        if (stack.item != Items.FIREWORK_ROCKET) {
            return false
        }
        val existing = stack.get(FIREWORKS)
        stack.set(
            FIREWORKS,
            Fireworks(
                existing?.flightDuration ?: 1,
                emptyList()
            )
        )
        player.inventoryMenu.broadcastChanges()
        return true
    }

    fun launchFireworks(
        player: ServerPlayer,
        power: Int,
        amount: Int,
        direction: Boolean = false,
    ): Int {
        val level = player.level()
        val template = ItemStack(Items.FIREWORK_ROCKET)
        template.set(
            FIREWORKS,
            Fireworks(if (direction) minOf(power, 1) else power, emptyList())
        )
        var launched = 0
        repeat(amount) {
            val rocket = if (direction) {
                FireworkRocketEntity(
                    level,
                    player.x,
                    player.eyeY,
                    player.z,
                    template.copy(),
                ).apply {
                    deltaMovement = player.lookAngle.normalize().scale(0.07)
                }
            } else {
                FireworkRocketEntity(level, template.copy(), player)
            }
            if (level.addFreshEntity(rocket)) {
                launched++
            }
        }
        return launched
    }

    /**
     * Essentials-style nuke: a 5x5 grid of primed TNT spawned 64 blocks above the highest block at
     * each column. The TNT is tagged so the `ExplosionEvent.PRE` handler registered in
     * `BootstrapLifecycle` turns its explosion into a zero-power, non-destructive effect.
     */
    fun nuke(level: ServerLevel, x: Double, z: Double) {
        val centerX = floor(x).toInt()
        val centerZ = floor(z).toInt()
        NukePattern.offsets.forEach { (dx, dz) ->
            val top = level.getHeight(
                Heightmap.Types.MOTION_BLOCKING,
                centerX + dx,
                centerZ + dz,
            )
            val tnt = PrimedTnt(
                level,
                (centerX + dx).toDouble(),
                (top + 64).toDouble(),
                (centerZ + dz).toDouble(),
                null,
            )
            tnt.addTag(NukeTntMarker.TAG)
            level.addFreshEntity(tnt)
        }
    }

    fun growTree(
        level: ServerLevel,
        pos: BlockPos,
        key: ResourceKey<ConfiguredFeature<*, *>>
    ): Boolean {
        val lookup = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE)
        val holder = lookup.getOrThrow(key)
        return holder.value().place(
            level,
            level.chunkSource.generator,
            level.random,
            pos,
        )
    }

    fun breakBlock(
        level: ServerLevel,
        pos: BlockPos,
        player: ServerPlayer
    ): Boolean =
        level.destroyBlock(
            pos,
            false,
            player,
            512
        )

    /** Applies the full powder-snow freeze effect without touching any water blocks. */
    fun freezePlayer(player: ServerPlayer) {
        player.ticksFrozen = player.ticksRequiredToFreeze
    }

    /** Clears the player's fire only, matching Essentials' `/ext`. */
    fun extinguishPlayer(player: ServerPlayer) {
        player.clearFire()
    }

    /** Visual-only lightning plus the manual damage Essentials applies unless the target is in god mode. */
    fun smite(player: ServerPlayer, power: Int) {
        if (player.isInvulnerable) {
            return
        }
        player.hurtServer(
            player.level(),
            player.level().damageSources().lightningBolt(),
            power.toFloat(),
        )
    }

    fun burn(target: LivingEntity, seconds: Double): Boolean {
        target.igniteForSeconds(seconds.toFloat())
        return true
    }

    fun stats(server: MinecraftServer): ServerStats {
        val runtime = Runtime.getRuntime()
        val averageNanos = server.averageTickTimeNanos
        val tps = when {
            averageNanos <= 0L -> 20.0
            else -> minOf(20.0, 1_000_000_000.0 / averageNanos.toDouble())
        }
        val worlds = server.allLevels.map { level ->
            var blockEntities = 0
            level.chunkSource.chunkMap.forEachReadyToSendChunk { chunk ->
                blockEntities += chunk.blockEntities.size
            }
            WorldStats(
                dimension = level.dimension().identifier().toString(),
                players = level.players().size,
                chunks = level.chunkSource.loadedChunksCount,
                entities = level.allEntities.count(),
                blockEntities = blockEntities,
            )
        }
        return ServerStats(
            uptimeTicks = server.tickCount.toLong(),
            tps = tps,
            averageTickMillis = averageNanos / 1_000_000.0,
            usedMemoryBytes = runtime.totalMemory() - runtime.freeMemory(),
            totalMemoryBytes = runtime.totalMemory(),
            maxMemoryBytes = runtime.maxMemory(),
            worlds = worlds,
        )
    }

    private companion object {

        private val FIREWORKS = DataComponents.FIREWORKS

    }

}
