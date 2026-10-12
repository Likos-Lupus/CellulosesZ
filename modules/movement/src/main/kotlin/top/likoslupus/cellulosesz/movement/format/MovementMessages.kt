package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.movement.home.DeleteHomeResult
import top.likoslupus.cellulosesz.movement.home.Home
import top.likoslupus.cellulosesz.movement.home.HomeLookupResult
import top.likoslupus.cellulosesz.movement.home.SetHomeResult
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.request.TeleportRequestType
import top.likoslupus.cellulosesz.movement.spawn.ClearSpawnResult
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome
import top.likoslupus.cellulosesz.movement.warp.DeleteWarpResult
import top.likoslupus.cellulosesz.movement.warp.SetWarpResult
import top.likoslupus.cellulosesz.movement.warp.Warp
import top.likoslupus.cellulosesz.movement.warp.WarpLookupResult
import kotlin.time.Duration

/**
 * Maps movement domain results to semantic values. It holds no language and no colour: the
 * presentation layer renders per recipient.
 */
internal object MovementMessages {

    /** Maps a teleport outcome to its failure notice, or null for success and silent cancellations. */
    fun teleportOutcome(outcome: TeleportOutcome): Notice? =
        when (outcome) {
            is TeleportOutcome.Success ->
                null

            TeleportOutcome.PlayerOffline ->
                notice(MessageKey("movement.teleport.error.player_offline"))

            TeleportOutcome.TargetOffline ->
                notice(MessageKey("movement.teleport.error.target_offline"))

            is TeleportOutcome.UnknownDimension ->
                notice(
                    MessageKey("movement.teleport.error.unknown_dimension"),
                    identifier(outcome.dimension)
                )

            TeleportOutcome.OutsideWorldBorder ->
                notice(MessageKey("movement.teleport.error.outside_border"))

            TeleportOutcome.UnsafeDestination ->
                notice(MessageKey("movement.teleport.error.unsafe"))

            is TeleportOutcome.Cooldown ->
                notice(
                    MessageKey("movement.teleport.error.cooldown"),
                    elapsed(outcome.remaining)
                )

            TeleportOutcome.AlreadyPending ->
                notice(MessageKey("movement.teleport.error.already_pending"))

            is TeleportOutcome.Cancelled ->
                cancellation(outcome.reason)

            TeleportOutcome.PassengerConflict ->
                notice(MessageKey("movement.teleport.error.passenger"))

            TeleportOutcome.RuntimeStopping ->
                notice(MessageKey("core.error.runtime_stopping"))
        }

    private fun cancellation(reason: TeleportCancellation): Notice? =
        when (reason) {
            TeleportCancellation.MOVED ->
                notice(MessageKey("movement.teleport.cancelled.moved"))

            TeleportCancellation.DAMAGED ->
                notice(MessageKey("movement.teleport.cancelled.damaged"))

            TeleportCancellation.DIMENSION_CHANGED ->
                notice(MessageKey("movement.teleport.cancelled.dimension"))

            TeleportCancellation.DISCONNECTED,
            TeleportCancellation.SERVER_STOPPING -> null
        }

    fun playerOffline(): Notice =
        notice(MessageKey("movement.teleport.error.player_offline"))

    fun teleportComplete(): Notice =
        success(MessageKey("movement.teleport.complete"))

    fun teleportBack(): Notice =
        success(MessageKey("movement.teleport.back"))

    fun noPreviousLocation(): Notice =
        notice(MessageKey("movement.teleport.no_previous"))

    fun teleportToHome(home: Home): Notice =
        success(MessageKey("movement.home.teleported"), identifier(home.name.value))

    fun homeSet(result: SetHomeResult): Notice =
        when (result) {
            SetHomeResult.Success ->
                success(MessageKey("movement.home.set"))

            SetHomeResult.InvalidName ->
                notice(MessageKey("movement.home.invalid_name"))

            SetHomeResult.LimitReached ->
                notice(MessageKey("movement.home.limit_reached"))
        }

    fun homeLookupError(result: HomeLookupResult): Notice? =
        when (result) {
            is HomeLookupResult.Found ->
                null

            HomeLookupResult.InvalidName ->
                notice(MessageKey("movement.home.invalid_name"))

            HomeLookupResult.NotFound ->
                notice(MessageKey("movement.home.not_found"))
        }

    fun homeDelete(name: String, result: DeleteHomeResult): Notice =
        when (result) {
            DeleteHomeResult.Deleted ->
                success(
                    MessageKey("movement.home.deleted"),
                    identifier(name)
                )

            DeleteHomeResult.InvalidName ->
                notice(MessageKey("movement.home.invalid_name"))

            DeleteHomeResult.NotFound ->
                notice(
                    MessageKey("movement.home.delete_not_found"),
                    identifier(name)
                )
        }

    fun homes(homes: List<Home>): ListPage =
        ListPage(
            title = LocalizedText(MessageKey("movement.home.list_title")),
            rows = homes.map { home ->
                ListRow(
                    main = LocalizedText(
                        MessageKey("core.value"),
                        listOf(identifier(home.name.value))
                    )
                )
            },
            emptyText = LocalizedText(MessageKey("movement.home.list_empty")),
        )

    fun teleportToWarp(warp: Warp): Notice =
        success(MessageKey("movement.warp.teleported"), identifier(warp.name.value))

    fun warpSet(name: String, result: SetWarpResult): Notice =
        when (result) {
            SetWarpResult.Success ->
                success(MessageKey("movement.warp.created"), identifier(name))

            SetWarpResult.InvalidName ->
                notice(MessageKey("movement.warp.invalid_name"))
        }

    fun warpLookupError(result: WarpLookupResult): Notice? =
        when (result) {
            is WarpLookupResult.Found ->
                null

            WarpLookupResult.InvalidName ->
                notice(MessageKey("movement.warp.invalid_name"))

            WarpLookupResult.NotFound ->
                notice(MessageKey("movement.warp.not_found"))
        }

    fun warpDelete(name: String, result: DeleteWarpResult): Notice =
        when (result) {
            DeleteWarpResult.Deleted ->
                success(
                    MessageKey("movement.warp.deleted"),
                    identifier(name)
                )

            DeleteWarpResult.InvalidName ->
                notice(MessageKey("movement.warp.invalid_name"))

            DeleteWarpResult.NotFound ->
                notice(
                    MessageKey("movement.warp.delete_not_found"),
                    identifier(name)
                )
        }

    fun warps(warps: List<Warp>): ListPage =
        ListPage(
            title = LocalizedText(MessageKey("movement.warp.list_title")),
            rows = warps.map { warp ->
                ListRow(
                    main = LocalizedText(
                        MessageKey("core.value"),
                        listOf(identifier(warp.name.value))
                    )
                )
            },
            emptyText = LocalizedText(MessageKey("movement.warp.list_empty")),
        )

    fun teleportToSpawn(): Notice =
        success(MessageKey("movement.spawn.teleported"))

    fun spawnSet(): Notice =
        success(MessageKey("movement.spawn.set"))

    fun spawnClear(result: ClearSpawnResult): Notice =
        when (result) {
            ClearSpawnResult.Cleared ->
                success(MessageKey("movement.spawn.cleared"))

            ClearSpawnResult.NotConfigured ->
                notice(MessageKey("movement.spawn.not_configured"))
        }

    fun requestSelf(): Notice =
        notice(MessageKey("movement.request.self"))

    fun requestAlreadyPending(): Notice =
        notice(MessageKey("movement.request.already_pending"))

    fun requestTargetQueueFull(target: String): Notice =
        notice(
            MessageKey("movement.request.target_queue_full"),
            playerName(target)
        )

    fun requestSent(target: String): Notice =
        success(
            MessageKey("movement.request.sent"),
            playerName(target)
        )

    fun requestRefreshed(target: String): Notice =
        success(
            MessageKey("movement.request.refreshed"),
            playerName(target)
        )

    fun requestReceived(type: TeleportRequestType, sender: String): Notice =
        Notice(
            text = LocalizedText(
                when (type) {
                    TeleportRequestType.TO_TARGET ->
                        MessageKey("movement.request.received.to_target")

                    TeleportRequestType.BRING_TARGET ->
                        MessageKey("movement.request.received.bring_target")
                },
                listOf(playerName(sender)),
            ),
            tone = NoticeTone.INFO,
            actions = listOf(
                UiAction.TeleportDecision(
                    LocalizedText(MessageKey("movement.request.accept")),
                    sender,
                    accept = true
                ),
                UiAction.TeleportDecision(
                    LocalizedText(MessageKey("movement.request.deny")),
                    sender,
                    accept = false
                ),
            ),
        )

    fun requestNone(): Notice =
        notice(MessageKey("movement.request.none"))

    fun requestAmbiguous(): Notice =
        notice(MessageKey("movement.request.ambiguous"))

    fun requestAccepted(): Notice =
        success(MessageKey("movement.request.accepted"))

    fun requestAcceptedNotify(subject: String): Notice =
        info(MessageKey("movement.request.accepted_notify"), playerName(subject))

    fun requestDenied(): Notice =
        success(MessageKey("movement.request.denied"))

    fun requestDeniedNotify(target: String): Notice =
        notice(MessageKey("movement.request.denied_notify"), playerName(target))

    fun requestCancelNone(): Notice =
        notice(MessageKey("movement.request.cancel_none"))

    fun requestCancelled(): Notice =
        success(MessageKey("movement.request.cancelled"))

    fun requestCancelledNotify(sender: String): Notice =
        info(MessageKey("movement.request.cancelled_notify"), playerName(sender))

    fun requestTargetOffline(): Notice =
        notice(MessageKey("movement.request.target_offline"))

    private fun notice(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

    private fun success(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    private fun info(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.INFO
        )

    private fun identifier(value: String): TextArgument =
        TextArgument.Identifier(value)

    private fun playerName(value: String): TextArgument =
        TextArgument.PlayerName(value)

    private fun elapsed(value: Duration): TextArgument =
        TextArgument.Elapsed(value)

}
