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
import top.likoslupus.cellulosesz.player.nearby.NearbyService
import top.likoslupus.cellulosesz.player.nickname.JdbcNicknameRepository
import top.likoslupus.cellulosesz.player.nickname.NicknameService
import top.likoslupus.cellulosesz.player.nickname.NicknameState
import top.likoslupus.cellulosesz.player.presence.PlayerPresenceService
import top.likoslupus.cellulosesz.player.session.JdbcPlayerSessionRepository
import top.likoslupus.cellulosesz.player.session.PlayerSessionService
import top.likoslupus.cellulosesz.player.session.SessionCloseKind
import java.time.Clock

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
        ) ?: return
        announce(change)
    }

    private fun announce(change: AfkChange) {
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
