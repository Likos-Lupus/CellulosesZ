package top.likoslupus.cellulosesz.movement.teleport.command

import net.minecraft.commands.CommandSourceStack
import top.likoslupus.cellulosesz.core.command.CommandFeedbackTarget
import top.likoslupus.cellulosesz.core.command.requiresPermission
import top.likoslupus.cellulosesz.core.permission.CommandPermissions
import top.likoslupus.cellulosesz.core.permission.PermissionService
import top.likoslupus.cellulosesz.core.preference.PlayerPreference
import top.likoslupus.cellulosesz.core.preference.PlayerPreferenceService
import top.likoslupus.cellulosesz.core.runtime.RuntimeKernel
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.adventure.AdventureRuntime
import top.likoslupus.cellulosesz.core.text.i18n.LocalizedMessages
import top.likoslupus.cellulosesz.core.text.presentation.TextPresentation
import top.likoslupus.cellulosesz.core.text.presentation.replyNotice
import top.likoslupus.cellulosesz.core.text.presentation.sendNotices
import top.likoslupus.cellulosesz.movement.command.movementLaunch
import top.likoslupus.cellulosesz.movement.config.TeleportSettings
import top.likoslupus.cellulosesz.movement.format.MovementMessages
import top.likoslupus.cellulosesz.movement.format.requestTargetDisabled
import top.likoslupus.cellulosesz.movement.format.teleportComplete
import top.likoslupus.cellulosesz.movement.format.teleportOutcome
import top.likoslupus.cellulosesz.movement.request.TeleportPreferenceGate
import top.likoslupus.cellulosesz.movement.teleport.*
import java.util.*

/** Builds the [TeleportIntent] for one teleport, resolving its policy from [teleportSettings]. */
internal fun teleportIntent(
    subjectId: UUID,
    position: StoredPosition,
    cause: TeleportCause,
    teleportSettings: () -> TeleportSettings,
): TeleportIntent =
    teleportIntent(
        subjectId,
        TeleportDestination.Fixed(position),
        cause,
        teleportSettings,
    )

/** Builds the [TeleportIntent] for one teleport, resolving its policy from [teleportSettings]. */
internal fun teleportIntent(
    subjectId: UUID,
    destination: TeleportDestination,
    cause: TeleportCause,
    teleportSettings: () -> TeleportSettings,
): TeleportIntent = TeleportIntent(
    subjectId = subjectId,
    destination = destination,
    cause = cause,
    policy = teleportPolicyFor(cause, teleportSettings()),
)

/**
 * Runs one admin teleport through the single [TeleportCoordinator] path. The `/tpo*` family is the
 * escape hatch for request toggles, not for preflight, safety or the per-world access gate.
 */
internal suspend fun executeAdminTeleport(
    subjectId: UUID,
    destination: TeleportDestination,
    teleports: TeleportCoordinator,
    teleportSettings: () -> TeleportSettings,
): TeleportOutcome = teleports.execute(
    teleportIntent(subjectId, destination, TeleportCause.ADMIN, teleportSettings)
)

/**
 * Vetoes a direct teleport when the other player has teleport requests disabled, unless the
 * sender holds the bypass capability. Replies the denial notice and returns its result, else null.
 */
internal fun disabledTeleportTarget(
    source: CommandSourceStack,
    playerId: UUID,
    playerName: String,
    permissions: PermissionService,
    preferences: PlayerPreferenceService,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
): Int? {
    val bypass = source.requiresPermission(
        permissions,
        CommandPermissions.TP_TOGGLE_BYPASS
    )
    val allowed = TeleportPreferenceGate.allowsTarget(
        preferences.isEnabled(
            playerId,
            PlayerPreference.TELEPORT_ENABLED
        ),
        bypass,
    )
    return when {
        allowed -> null
        else -> source.replyNotice(
            adventure,
            presentation,
            source.player?.uuid,
            MovementMessages.requestTargetDisabled(playerName),
        )
    }
}

/** Runs one subject→player teleport through [TeleportCoordinator]. */
internal fun launchToPlayer(
    source: CommandSourceStack,
    subjectId: UUID,
    targetId: UUID,
    cause: TeleportCause,
    teleports: TeleportCoordinator,
    teleportSettings: () -> TeleportSettings,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
): Int = launchDestination(
    source,
    subjectId,
    TeleportDestination.Player(targetId),
    cause,
    teleports,
    teleportSettings,
    kernel,
    messages,
    adventure,
    presentation,
)

/** Runs one subject→fixed-position teleport through [TeleportCoordinator]. */
internal fun launchToPosition(
    source: CommandSourceStack,
    subjectId: UUID,
    position: StoredPosition,
    cause: TeleportCause,
    teleports: TeleportCoordinator,
    teleportSettings: () -> TeleportSettings,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
): Int = launchDestination(
    source,
    subjectId,
    TeleportDestination.Fixed(position),
    cause,
    teleports,
    teleportSettings,
    kernel,
    messages,
    adventure,
    presentation,
)

/**
 * Runs one direct teleport through the shared pending-feedback wrapper and reports the
 * coordinator's outcome to the subject.
 */
internal fun launchDestination(
    source: CommandSourceStack,
    subjectId: UUID,
    destination: TeleportDestination,
    cause: TeleportCause,
    teleports: TeleportCoordinator,
    teleportSettings: () -> TeleportSettings,
    kernel: RuntimeKernel,
    messages: LocalizedMessages,
    adventure: AdventureRuntime,
    presentation: TextPresentation,
): Int =
    movementLaunch(
        source,
        kernel,
        messages,
        adventure,
        MessageKey("movement.pending.teleport")
    ) { _ ->
        val outcome = teleports.execute(
            teleportIntent(
                subjectId,
                destination,
                cause,
                teleportSettings
            )
        )
        val notices = when (outcome) {
            is TeleportOutcome.Success -> listOf(MovementMessages.teleportComplete())
            else -> listOfNotNull(MovementMessages.teleportOutcome(outcome))
        }
        kernel.sendNotices(
            CommandFeedbackTarget.Player(subjectId),
            adventure,
            presentation,
            subjectId,
            notices,
        )
    }
