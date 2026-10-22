package top.likoslupus.cellulosesz.application.bootstrap

import dev.architectury.event.EventResult
import dev.architectury.event.events.common.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.Level
import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.application.ApplicationSqlSchema
import top.likoslupus.cellulosesz.application.health.ApplicationHealth
import top.likoslupus.cellulosesz.communication.CommunicationSqlSchema
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.SleepExclusionRegistry
import top.likoslupus.cellulosesz.core.player.TemporaryProtectionRegistry
import top.likoslupus.cellulosesz.core.player.identity.IdentitySqlSchema
import top.likoslupus.cellulosesz.core.preference.PreferenceSqlSchema
import top.likoslupus.cellulosesz.core.runtime.KernelState
import top.likoslupus.cellulosesz.core.text.i18n.preference.LocalizationSqlSchema
import top.likoslupus.cellulosesz.economy.EconomySqlSchema
import top.likoslupus.cellulosesz.foundation.database.schema.SqlSchemaContributor
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import top.likoslupus.cellulosesz.player.PlayerSqlSchema
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import java.time.Instant

private val businessContributors: List<SqlSchemaContributor> = listOf(
    IdentitySqlSchema,
    LocalizationSqlSchema,
    MovementSqlSchema,
    CommunicationSqlSchema,
    AdministrationSqlSchema,
    UtilitySqlSchema,
    PlayerSqlSchema,
    EconomySqlSchema,
    ApplicationSqlSchema,
    PreferenceSqlSchema,
)

/**
 * Bootstrap phase four: register every Architectury lifecycle, tick, damage, chat and player event
 * in the original order. Storage bring-up, feature startup/shutdown and the API session are all
 * driven from these callbacks.
 */
internal object BootstrapLifecycle {

    fun register(state: BootstrapState) {
        LifecycleEvent.SERVER_STARTING.register { server ->
            state.kernel.onServerStarting(server)
            state.adventure.start(server)
            state.health.transition(ApplicationHealth.State.BOOTSTRAPPING_STORAGE)

            val bootstrap = StorageBootstrap(
                BootstrapConfiguration.dataRoot(state.kernel),
                BootstrapConfiguration.gameRoot(),
                businessContributors
            )
            val result = try {
                bootstrap.startBlocking(state.config.current.database)
            } catch (failure: Throwable) {
                state.health.fail(failure.message ?: failure.javaClass.simpleName)
                throw failure
            }
            state.database.initialize(result.database)
            state.commandCooldowns.hydrateBlocking()
            state.health.storageType = result.descriptor.type.name.lowercase()
            state.health.storageEndpoint = result.descriptor.redactedEndpoint
            state.health.driverSource = result.driverSource.name.lowercase()
            state.health.migration = result.migration
            state.health.transition(ApplicationHealth.State.STORAGE_READY)

            val identities = state.identityRepository.loadAllBlocking()
            state.known.hydrate(identities.map { KnownPlayerIdentity(it.id, it.name) })
            state.health.identityRecords = identities.size

            state.languagePreferences.hydrate(state.languageRepository.loadAllBlocking())
            state.preferences.hydrate(state.preferenceRepository.loadAllBlocking())

            state.movement.onServerStarting()
            state.administration.onServerStarting()
            state.communication.onServerStarting()
            state.utility.onServerStarting()
            state.playerFeature.onServerStarting()
            state.economy.onServerStarting()
            BootstrapApi.openSession(state)
            state.health.transition(ApplicationHealth.State.LOADING_STATE)
        }
        LifecycleEvent.SERVER_STARTED.register {
            state.kernel.onServerStarted()
            state.backup.onServerStarted()
            state.health.transition(ApplicationHealth.State.READY)
            state.apiProvider.markReady()
        }
        LifecycleEvent.SERVER_STOPPING.register {
            state.health.transition(ApplicationHealth.State.STOPPING)
            state.backup.onServerStopping()
            state.apiProvider.markStopping()
            state.sessionEvents.getAndSet(null)?.clear()
            state.movement.onServerStopping()
            state.communication.onServerStopping()
            state.administration.onServerStopping()
            state.utility.onServerStopping()
            state.playerFeature.onServerStopping()
            state.economy.onServerStopping()
            TemporaryProtectionRegistry.clearAll()
            SleepExclusionRegistry.clear()
            state.database.close()
            state.kernel.onServerStopping()
            state.health.transition(ApplicationHealth.State.STOPPED)
        }
        LifecycleEvent.SERVER_STOPPED.register {
            state.adventure.stop()
            state.kernel.onServerStopped()
            state.apiProvider.markStopped()
        }

        TickEvent.PLAYER_POST.register { player ->
            if (player is ServerPlayer) {
                state.movement.onPlayerTick(player)
                state.administration.onPlayerTick(player)
                state.playerFeature.onPlayerTick(player)
                state.utility.onPlayerTick(player)
            }
        }
        EntityEvent.LIVING_HURT.register { entity, source, _ ->
            if (entity is ServerPlayer) {
                val attackerId = (source.entity as? ServerPlayer)?.uuid
                if (attackerId != null
                    && attackerId != entity.uuid
                    && state.administration.isLoginProtected(attackerId, Instant.now())
                ) {
                    return@register EventResult.interruptFalse()
                }
                if (TemporaryProtectionRegistry.isProtected(entity.uuid, Instant.now())) {
                    return@register EventResult.interruptFalse()
                }
                state.movement.onPlayerHurt(entity.uuid)
            }
            EventResult.pass()
        }

        EntityEvent.LIVING_DEATH.register { entity, _ ->
            if (entity is ServerPlayer) {
                state.movement.recordDeathLocation(
                    playerId = entity.uuid,
                    dimension = entity.level().dimension().identifier().toString(),
                    x = entity.x,
                    y = entity.y,
                    z = entity.z,
                    yaw = entity.yRot,
                    pitch = entity.xRot,
                )
            }
            EventResult.pass()
        }

        // `/nuke` TNT is tagged; cancel its real explosion and replay a zero-power, non-destructive
        // one. Passing a null source keeps this PRE handler from matching its own replay.
        ExplosionEvent.PRE.register { level, explosion ->
            if (state.administration.isNukeTntSource(explosion.directSourceEntity)) {
                val center = explosion.center()
                level.explode(
                    null,
                    center.x,
                    center.y,
                    center.z,
                    0.0f,
                    Level.ExplosionInteraction.NONE,
                )
                EventResult.interruptFalse()
            } else {
                EventResult.pass()
            }
        }
        PlayerEvent.PLAYER_JOIN.register { player ->
            state.administration.onPlayerJoined(player)
            state.communication.onPlayerJoined(player.uuid)
            state.playerFeature.onPlayerJoined(player)
            val identity = KnownPlayerIdentity(player.uuid, player.gameProfile.name)
            state.known.record(identity)
            state.kernel.launch {
                runCatching {
                    state.identityRepository.observe(
                        identity,
                        Instant.now()
                    )
                }
            }
        }

        ChatEvent.RECEIVED.register { player, _ ->
            if (player != null
                && state.administration.isMuted(player.uuid)
            ) {
                state.presentation.renderFor(
                    player.uuid,
                    state.administration.mutedChatNotice()
                ).forEach {
                    state.adventure.audience(
                        CommandFeedbackTarget.Player(player.uuid)
                    ).sendMessage(it)
                }
                EventResult.interruptFalse()
            } else {
                player?.let { state.playerFeature.onChat(it.uuid) }
                EventResult.pass()
            }
        }

        PlayerEvent.PICKUP_ITEM_PRE.register { player, _, _ ->
            if (player is ServerPlayer
                && state.playerFeature.blocksItemPickup(player.uuid)
            ) {
                EventResult.interruptFalse()
            } else {
                EventResult.pass()
            }
        }

        PlayerEvent.PLAYER_QUIT.register { player ->
            state.administration.onPlayerQuit(player.uuid)
            state.communication.onPlayerQuit(player.uuid)
            state.utility.onPlayerQuit(player.uuid)
            state.playerFeature.onPlayerQuit(player)
            state.economy.onPlayerQuit(player.uuid)
            val affected = state.movement.onPlayerQuit(player.uuid)
            if (state.kernel.state == KernelState.RUNNING) {
                affected.forEach { senderId ->
                    state.presentation.renderFor(
                        senderId,
                        state.movement.requestTargetOfflineNotice()
                    ).forEach {
                        state.adventure
                                .audience(CommandFeedbackTarget.Player(senderId))
                                .sendMessage(it)
                    }
                }
            }
        }
    }

}
