package top.likoslupus.cellulosesz.administration

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.administration.config.AdministrationSettings
import top.likoslupus.cellulosesz.administration.jail.JailCommandAccessPolicy
import top.likoslupus.cellulosesz.administration.jail.JailRespawnState
import top.likoslupus.cellulosesz.administration.jail.JailService
import top.likoslupus.cellulosesz.administration.jail.RestrictCommandsState
import top.likoslupus.cellulosesz.administration.login.LoginProtection
import top.likoslupus.cellulosesz.administration.moderation.ban.BanService
import top.likoslupus.cellulosesz.administration.moderation.mute.MuteService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerAdjustmentService
import top.likoslupus.cellulosesz.administration.playerstate.PlayerStateService
import top.likoslupus.cellulosesz.administration.sign.SignEditService
import top.likoslupus.cellulosesz.administration.socialspy.SocialSpyService
import top.likoslupus.cellulosesz.administration.vanish.VanishService
import top.likoslupus.cellulosesz.administration.worldtools.PersonalViews
import top.likoslupus.cellulosesz.administration.worldtools.WorldToolsService
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.core.player.MinecraftKnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.TemporaryProtectionRegistry
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import java.time.Instant
import java.util.*

/**
 * Server lifecycle and jail-confinement collaborator of [AdministrationFeature]. Every member must
 * run on the server thread; the facade keeps the same public entry points and delegates here.
 */
internal class AdministrationLifecycle(
    private val kernel: RuntimeKernel,
    private val settings: () -> AdministrationSettings,
    private val known: MinecraftKnownPlayerResolver,
    private val bans: BanService,
    private val mutes: MuteService,
    private val jail: JailService,
    private val vanish: VanishService,
    private val spies: SocialSpyService,
    private val adjustments: PlayerAdjustmentService,
    private val playerState: PlayerStateService,
    private val personalViews: PersonalViews,
    private val worldTools: WorldToolsService,
    private val signEdits: SignEditService,
    private val login: LoginProtection,
    private val permissions: PermissionService,
) {

    private val lastDimensions = HashMap<UUID, String>()

    /**
     * Re-publishes the jail command-restriction snapshot from config. The composition root calls
     * this after a reload; [onServerStarting] also seeds it, so it is never stale for a running
     * server. Must be called on the server thread.
     */
    fun applyJailCommandRestrictions() {
        RestrictCommandsState.set(JailCommandAccessPolicy.snapshotFrom(settings().jail))
    }

    /** Seeds known identities and loads durable mutes. Must run when a server reference exists. */
    fun onServerStarting() {
        RestrictCommandsState.set(JailCommandAccessPolicy.snapshotFrom(settings().jail))
        vanish.applyPolicy()
        kernel.launch {
            kernel.onServerThread {
                val server = kernel.requireServer()
                known.clear()
                server.playerList.players.forEach { player ->
                    known.record(
                        KnownPlayerIdentity(
                            player.uuid,
                            player.gameProfile.name
                        )
                    )
                }
                bans.listedAccountIdentities().forEach {
                    known.record(KnownPlayerIdentity(it.id, it.name))
                }
            }

            mutes.load()
            jail.load()

            kernel.onServerThread {
                mutes.activeMutes().forEach { mute ->
                    known.record(
                        KnownPlayerIdentity(
                            mute.playerId,
                            mute.playerName
                        )
                    )
                }
            }
        }
    }

    fun onServerStopping() {
        mutes.shutdown()
        jail.shutdown()
        vanish.clearAll()
        adjustments.restoreAllSpeeds()
        playerState.clearAll()
        personalViews.clearAll()
        signEdits.clearAll()
        login.clear()
        lastDimensions.clear()
    }

    /** Records every joined player so offline moderation can resolve them later. */
    fun onPlayerJoined(player: ServerPlayer) {
        known.record(
            KnownPlayerIdentity(
                player.uuid,
                player.gameProfile.name
            )
        )
        val now = Instant.now()
        login.recordJoin(player.uuid, now)
        val delay = settings().moderation.loginAttackDelaySeconds
        if (delay > 0) {
            TemporaryProtectionRegistry.protect(
                player.uuid,
                now.plusSeconds(delay.toLong()),
            )
        }
        if (settings().playerAdjustments.flyOnJoin &&
            player.createCommandSourceStack()
                    .requiresPermission(permissions, CommandPermissions.FLY)
        ) {
            playerState.grantFly(player.uuid, true)
        }
        jail.onPlayerJoined(player.uuid)
        resendPersonalViews(player)
    }

    fun onPlayerQuit(playerId: UUID) {
        spies.clear(playerId)
        vanish.clear(playerId)
        adjustments.restoreSpeed(playerId)
        playerState.onPlayerQuit(playerId)
        personalViews.clear(playerId)
        signEdits.clear(playerId)
        login.forget(playerId)
        jail.onPlayerQuit(playerId)
        lastDimensions.remove(playerId)
    }

    /**
     * Re-sends a player's personal time and weather to their client. Called on join and whenever
     * their dimension changes, because the client resets both when it loads a new world.
     */
    fun resendPersonalViews(player: ServerPlayer) {
        personalViews.time(player.uuid)?.let {
            worldTools.sendPersonalTime(player, it)
        }
        personalViews.weather(player.uuid)?.let {
            worldTools.sendPersonalWeather(player, it)
        }
    }

    /**
     * Server-thread confinement tick. Re-anchors jailed players that left the allowed radius and
     * drives release-pending players out. Throttled to `confinementCheckIntervalTicks`, except for a
     * player that just respawned, who is re-checked on the next tick. It never consults
     * `[administration.jail].enabled`, so existing sentences keep being enforced when the capability
     * is disabled.
     */
    fun onPlayerTick(player: ServerPlayer) {
        playerState.onPlayerObserved(player.uuid)
        jail.onOnlineTick(player.uuid)
        val dimension = player.level().dimension().identifier().toString()
        val previousDimension = lastDimensions.put(player.uuid, dimension)
        if (previousDimension != null && previousDimension != dimension) {
            resendPersonalViews(player)
        }
        val config = settings().jail
        val server = kernel.requireServer()
        val respawned = JailRespawnState.consume(player.uuid)
        val interval = config.confinementCheckIntervalTicks
        if (!respawned && (interval <= 0 || server.tickCount % interval != 0)) {
            return
        }

        jail.onObservation(
            playerId = player.uuid,
            dimension = player.level().dimension().identifier().toString(),
            x = player.x,
            y = player.y,
            z = player.z,
        )
    }
}
