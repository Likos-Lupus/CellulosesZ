package top.likoslupus.cellulosesz.player

import net.minecraft.server.level.ServerPlayer
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.dsl.CommandDefinition
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.permission.VanillaFallbackPermissionBridge
import top.likoslupus.cellulosesz.core.player.KnownPlayerResolver
import top.likoslupus.cellulosesz.core.player.identity.PlayerIdentityRepository
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.runtime.serverThreadRunner
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.player.afk.AfkChange
import top.likoslupus.cellulosesz.player.afk.AfkService
import top.likoslupus.cellulosesz.player.command.*
import top.likoslupus.cellulosesz.player.config.PlayerSettings
import top.likoslupus.cellulosesz.player.format.PlayerMessages
import top.likoslupus.cellulosesz.player.integration.*
import top.likoslupus.cellulosesz.player.nearby.NearbyService
import top.likoslupus.cellulosesz.player.nickname.*
import top.likoslupus.cellulosesz.player.presence.PlayerPresenceService
import top.likoslupus.cellulosesz.player.presence.PresenceLookup
import top.likoslupus.cellulosesz.player.session.JdbcPlayerSessionRepository
import top.likoslupus.cellulosesz.player.session.PlayerSessionService
import top.likoslupus.cellulosesz.player.session.SessionCloseKind
import java.time.Clock
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.toJavaDuration

/**
 * Public surface of the player bounded context (presence, sessions, nicknames, player queries).
 * Internals stay `internal`.
 */
public class PlayerFeature internal constructor(
    private val sessions: PlayerSessionService,
    private val presence: PlayerPresenceService,
    private val afk: AfkService,
    private val nearby: NearbyService,
    private val nicknames: NicknameService,
    private val kernel: RuntimeKernel,
    private val permissions: PermissionService,
    private val messages: LocalizedMessages,
    private val adventure: AdventureRuntime,
    private val presentation: TextPresentation,
    private val settings: () -> PlayerSettings,
    private val clock: Clock,
) {

    private val onlineSnapshot = AtomicReference<Set<UUID>>(emptySet())
    private val afkSnapshot = AtomicReference<Map<UUID, Boolean>>(emptyMap())

    /** Receives committed player-domain facts; set by the composition root. */
    public var domainObserver: PlayerDomainObserver = PlayerDomainObserver.None

    /** Narrow integration surface for the public API; never exposes the internal services. */
    public val integration: PlayerIntegrationPort = PlayerIntegration()

    private inner class PlayerIntegration : PlayerIntegrationPort {

        override fun online(playerId: UUID): Boolean =
            playerId in onlineSnapshot.get()

        override fun afk(playerId: UUID): Boolean? =
            when (playerId) {
                !in onlineSnapshot.get() -> null
                else -> afkSnapshot.get()[playerId] ?: false
            }

        override suspend fun profile(playerId: UUID): PlayerProfileResult =
            when (val lookup = presence.lookupById(playerId)) {
                is PresenceLookup.Found -> PlayerProfileResult.Found(
                    PlayerProfileSnapshot(
                        playerId = lookup.presence.id,
                        lastKnownName = lookup.presence.name,
                        online = lookup.presence.online,
                        firstSeen = lookup.presence.firstSeen,
                        lastSeen = lookup.presence.lastSeen,
                        playtime = lookup.presence.playtime.toJavaDuration(),
                    )
                )

                PresenceLookup.NotFound -> PlayerProfileResult.Unknown
                PresenceLookup.StorageUnavailable -> PlayerProfileResult.StorageUnavailable
            }

        override suspend fun nickname(playerId: UUID): NicknameResult =
            when (val current = nicknames.current(playerId)) {
                is NicknameCurrent.Set -> NicknameResult.Current(current.display)
                NicknameCurrent.None -> NicknameResult.Current(null)
                NicknameCurrent.StorageUnavailable -> NicknameResult.StorageUnavailable
            }

        override suspend fun changeOwnNickname(
            actorId: UUID,
            newName: String?,
        ): NicknameChangeResult {
            val previous = (nicknames.current(actorId) as? NicknameCurrent.Set)?.display
            val result = when (newName) {
                null -> when (nicknames.clear(actorId)) {
                    NicknameSetResult.Cleared -> NicknameChangeResult.Changed(null)
                    NicknameSetResult.Disabled -> NicknameChangeResult.Disabled
                    NicknameSetResult.StorageUnavailable -> NicknameChangeResult.StorageUnavailable
                    else -> NicknameChangeResult.InvalidName
                }

                else -> when (
                    val set = nicknames.set(
                        actorId,
                        newName,
                        actorId
                    )
                ) {
                    is NicknameSetResult.Set -> NicknameChangeResult.Changed(set.display)
                    NicknameSetResult.Disabled -> NicknameChangeResult.Disabled
                    NicknameSetResult.Reserved -> NicknameChangeResult.Reserved
                    NicknameSetResult.Taken -> NicknameChangeResult.NameTaken
                    NicknameSetResult.StorageUnavailable -> NicknameChangeResult.StorageUnavailable
                    else -> NicknameChangeResult.InvalidName
                }
            }
            if (result is NicknameChangeResult.Changed) {
                domainObserver.onNicknameChanged(
                    actorId,
                    previous,
                    result.name
                )
            }
            return result
        }

    }

    public fun commands(): List<CommandDefinition> =
        SessionCommands.commands(
            presence,
            kernel,
            messages,
            adventure,
            presentation,
            clock
        ) + AfkCommands.commands(
            afk,
            adventure,
            presentation,
            ::announce
        ) + PingCommands.commands(
            adventure,
            presentation
        ) + NearbyCommands.commands(
            nearby,
            permissions,
            adventure,
            presentation
        ) + NicknameCommands.commands(
            nicknames,
            kernel,
            messages,
            adventure,
            presentation
        )

    /** Recovers sessions left open by a previous process and starts heartbeats. */
    public fun onServerStarting() {
        sessions.onServerStarting()
        applyNicknameDisplay()
    }

    /** Bounded blocking flush of open sessions before the database closes. */
    public fun onServerStopping() {
        sessions.finishAllBlocking()
        afk.clearAll()
        nicknames.clearAllDisplays()
    }

    /** Begins a fresh play session and hydrates the nickname display. Must run on the server thread. */
    public fun onPlayerJoined(player: ServerPlayer) {
        sessions.begin(player.uuid)
        onlineSnapshot.set(onlineSnapshot.get() + player.uuid)
        afkSnapshot.set(afkSnapshot.get() + (player.uuid to false))
        domainObserver.onPresenceChanged(player.uuid, true)
        applyNicknameDisplay()
        kernel.launch { nicknames.loadDisplay(player.uuid) }
    }

    /**
     * Publishes the nickname vanilla-display policy from config. The composition root may call this
     * after a config reload; onServerStarting and every join also refresh it, so the flag never goes
     * stale for the running server.
     */
    public fun applyNicknameDisplay() {
        NicknameState.setApplyToVanillaDisplay(settings().nicknames.applyToVanillaDisplay)
    }

    /** Closes the play session with NORMAL and clears session-only state. Server thread. */
    public fun onPlayerQuit(player: ServerPlayer) {
        sessions.end(player.uuid, SessionCloseKind.NORMAL)
        afk.clear(player.uuid)
        nicknames.clearDisplay(player.uuid)
        onlineSnapshot.set(onlineSnapshot.get() - player.uuid)
        afkSnapshot.set(afkSnapshot.get() - player.uuid)
        domainObserver.onPresenceChanged(player.uuid, false)
    }

    /** Server-thread tick hook that drives auto-AFK. */
    public fun onPlayerTick(player: ServerPlayer) {
        val change = afk.onTick(
            playerId = player.uuid,
            playerName = player.gameProfile.name,
            x = player.x,
            y = player.y,
            z = player.z,
            yaw = player.yRot,
            pitch = player.xRot,
        )
        afkSnapshot.set(afkSnapshot.get() + (player.uuid to afk.isAfk(player.uuid)))
        if (change != null) {
            announce(change)
        }
    }

    private fun announce(change: AfkChange) {
        domainObserver.onAfkChanged(
            change.playerId,
            change.afk,
            PlayerAfkCause.OTHER
        )
        if (!settings().afk.announceStateChanges) {
            return
        }
        val notice = PlayerMessages.afkAnnouncement(change)
        kernel.launch {
            val recipients = kernel.onServerThread {
                kernel.requireServer().playerList.players.map { it.uuid }
            }
            recipients.forEach { id ->
                kernel.sendNotices(
                    CommandFeedbackTarget.Player(id),
                    adventure,
                    presentation,
                    id,
                    listOf(notice)
                )
            }
        }
    }

}

public fun createPlayerFeature(
    kernel: RuntimeKernel,
    database: DatabaseRuntime,
    namespace: String,
    settings: () -> PlayerSettings,
    known: KnownPlayerResolver,
    identities: PlayerIdentityRepository,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
    permissions: PermissionService = PermissionService(VanillaFallbackPermissionBridge),
): PlayerFeature {
    val clock = Clock.systemUTC()
    val sessions = PlayerSessionService(
        kernel = kernel,
        repository = JdbcPlayerSessionRepository(database, namespace),
        settings = { settings().sessions },
        clock = clock,
    )
    return PlayerFeature(
        sessions = sessions,
        presence = PlayerPresenceService(
            kernel = kernel,
            known = known,
            identities = identities,
            sessions = sessions,
            clock = clock,
        ),
        afk = AfkService(
            settings = { settings().afk },
            clock = clock,
        ),
        nearby = NearbyService(
            kernel = kernel,
            settings = { settings().nearby },
        ),
        nicknames = NicknameService(
            runner = kernel.serverThreadRunner(),
            known = known,
            identities = identities,
            repository = JdbcNicknameRepository(database, namespace),
            settings = { settings().nicknames },
            clock = clock,
        ),
        kernel = kernel,
        permissions = permissions,
        messages = messages,
        adventure = adventure,
        presentation = presentation,
        settings = settings,
        clock = clock,
    )
}
