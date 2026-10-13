package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.movement.home.*
import top.likoslupus.cellulosesz.movement.pending.TeleportCancellation
import top.likoslupus.cellulosesz.movement.random.*
import top.likoslupus.cellulosesz.movement.request.*
import top.likoslupus.cellulosesz.movement.spawn.ClearSpawnResult
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import top.likoslupus.cellulosesz.movement.teleport.TeleportOutcome
import top.likoslupus.cellulosesz.movement.terrain.TerrainResult
import top.likoslupus.cellulosesz.movement.terrain.TerrainTarget
import top.likoslupus.cellulosesz.movement.warp.*
import java.time.Instant
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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

            TeleportOutcome.Confined ->
                notice(MessageKey("movement.teleport.error.confined"))

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

    fun requestSenderQueueFull(): Notice =
        notice(MessageKey("movement.request.sender_queue_full"))

    fun requestServerQueueFull(): Notice =
        notice(MessageKey("movement.request.server_queue_full"))

    fun requestBatchSent(count: Int): Notice =
        success(MessageKey("movement.request.batch_sent"), count(count.toLong()))

    fun requestBatchNone(): Notice =
        notice(MessageKey("movement.request.batch_none"))

    fun requestBatchCooldown(remaining: Duration): Notice =
        notice(MessageKey("movement.request.batch_cooldown"), elapsed(remaining))

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

    fun tpAutoStatus(mode: TpAutoMode): Notice =
        when (mode) {
            TpAutoMode.OFF ->
                notice(MessageKey("movement.tpauto.mode.off"))

            TpAutoMode.TRUSTED ->
                success(MessageKey("movement.tpauto.mode.trusted"))

            TpAutoMode.ALL ->
                success(MessageKey("movement.tpauto.mode.all"))
        }

    fun tpAutoHelp(): Notice =
        info(MessageKey("movement.tpauto.help"))

    fun tpAutoToggle(result: TpAutoToggleResult): Notice =
        when (result) {
            is TpAutoToggleResult.Updated -> tpAutoStatus(result.mode)
            TpAutoToggleResult.StorageUnavailable ->
                notice(MessageKey("movement.tpauto.storage_unavailable"))
        }

    fun tpAutoAllow(target: String, result: TpAutoAllowResult): Notice =
        when (result) {
            TpAutoAllowResult.Added ->
                success(MessageKey("movement.tpauto.added"), playerName(target))

            TpAutoAllowResult.AlreadyAllowed ->
                notice(MessageKey("movement.tpauto.already_allowed"), playerName(target))

            TpAutoAllowResult.Self ->
                notice(MessageKey("movement.tpauto.self"))

            TpAutoAllowResult.LimitReached ->
                notice(MessageKey("movement.tpauto.limit_reached"))

            TpAutoAllowResult.StorageUnavailable ->
                notice(MessageKey("movement.tpauto.storage_unavailable"))
        }

    fun tpAutoDisallow(target: String, result: TpAutoDisallowResult): Notice =
        when (result) {
            TpAutoDisallowResult.Removed ->
                success(MessageKey("movement.tpauto.removed"), playerName(target))

            TpAutoDisallowResult.NotAllowed ->
                notice(MessageKey("movement.tpauto.not_allowed"), playerName(target))

            TpAutoDisallowResult.StorageUnavailable ->
                notice(MessageKey("movement.tpauto.storage_unavailable"))
        }

    fun tpAutoAutoAccepted(sender: String): Notice =
        info(MessageKey("movement.tpauto.auto_accepted"), playerName(sender))

    fun tpAutoList(names: List<String>): ListPage =
        ListPage(
            title = LocalizedText(MessageKey("movement.tpauto.list_title")),
            rows = names.map { name ->
                ListRow(main = LocalizedText(MessageKey("core.value"), listOf(playerName(name))))
            },
            emptyText = LocalizedText(MessageKey("movement.tpauto.list_empty")),
        )

    fun homeRename(from: String, to: String, result: RenameHomeResult): Notice =
        when (result) {
            RenameHomeResult.Renamed ->
                success(
                    MessageKey("movement.home.renamed"),
                    identifier(from),
                    identifier(to),
                )

            RenameHomeResult.InvalidName ->
                notice(MessageKey("movement.home.invalid_name"))

            RenameHomeResult.SourceNotFound ->
                notice(
                    MessageKey("movement.home.rename_source_not_found"),
                    identifier(from),
                )

            RenameHomeResult.DestinationExists ->
                notice(
                    MessageKey("movement.home.rename_destination_exists"),
                    identifier(to),
                )
        }

    fun warpInfo(
        found: WarpInfoResult.Found,
        now: Instant,
        showCoordinates: Boolean,
    ): DetailView {
        val warp = found.warp
        val metadata = found.metadata
        val fields = buildList {
            add(
                DetailField(
                    label = LocalizedText(MessageKey("movement.warp.info.field.dimension")),
                    value = LocalizedText(
                        MessageKey("core.value"),
                        listOf(literal(warp.position.dimension)),
                    ),
                )
            )
            if (!showCoordinates) {
                return@buildList
            }
            add(
                DetailField(
                    label = LocalizedText(MessageKey("movement.warp.info.field.position")),
                    value = LocalizedText(
                        MessageKey("core.position.dimension"),
                        listOf(blockPosition(warp.position)),
                    ),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("movement.warp.info.field.created_by")),
                    value = metadata
                            ?.let {
                                LocalizedText(
                                    MessageKey("core.value"),
                                    listOf(playerName(it.createdBy))
                                )
                            }
                        ?: LocalizedText(MessageKey("movement.warp.info.unknown")),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("movement.warp.info.field.created_at")),
                    value = metadata
                            ?.let {
                                LocalizedText(
                                    MessageKey("core.value"),
                                    listOf(elapsed(ago(it.createdAt, now))),
                                )
                            }
                        ?: LocalizedText(MessageKey("movement.warp.info.unknown")),
                )
            )
            add(
                DetailField(
                    label = LocalizedText(MessageKey("movement.warp.info.field.updated_at")),
                    value = metadata
                            ?.updatedAt
                            ?.let {
                                LocalizedText(
                                    MessageKey("core.value"),
                                    listOf(elapsed(ago(it, now))),
                                )
                            }
                        ?: LocalizedText(MessageKey("movement.warp.info.unknown")),
                )
            )
        }
        return DetailView(
            title = LocalizedText(
                MessageKey("movement.warp.info.title"),
                listOf(identifier(warp.name.value)),
            ),
            fields = fields,
        )
    }

    fun randomTeleported(): Notice =
        success(MessageKey("movement.random.teleported"))

    fun randomTeleportError(result: RandomTeleportResult): Notice? =
        when (result) {
            is RandomTeleportResult.Found ->
                null

            RandomTeleportResult.Disabled ->
                notice(MessageKey("movement.random.disabled"))

            RandomTeleportResult.NotConfigured ->
                notice(MessageKey("movement.random.not_configured"))

            is RandomTeleportResult.UnknownRegion ->
                notice(MessageKey("movement.random.unknown_region"), identifier(result.name))

            is RandomTeleportResult.WrongDimension ->
                notice(MessageKey("movement.random.wrong_dimension"), identifier(result.name))

            is RandomTeleportResult.RegionDisabled ->
                notice(MessageKey("movement.random.region_disabled"), identifier(result.name))

            is RandomTeleportResult.NoValidArea ->
                notice(MessageKey("movement.random.no_area"), identifier(result.name))

            RandomTeleportResult.RegionChanged ->
                notice(MessageKey("movement.random.region_changed"))

            RandomTeleportResult.SearchFailed ->
                notice(MessageKey("movement.random.failed"))

            RandomTeleportResult.Busy ->
                notice(MessageKey("movement.random.busy"))

            is RandomTeleportResult.Cooldown ->
                notice(
                    MessageKey("movement.random.cooldown"),
                    elapsed(result.remaining),
                )

            RandomTeleportResult.PlayerOffline ->
                notice(MessageKey("movement.teleport.error.player_offline"))
        }

    fun randomRegionSet(name: String, result: SetRegionResult): Notice =
        when (result) {
            SetRegionResult.Saved ->
                success(MessageKey("movement.random.region_set"), identifier(name))

            SetRegionResult.InvalidName ->
                notice(MessageKey("movement.random.region_invalid_name"))

            SetRegionResult.InvalidRange ->
                notice(MessageKey("movement.random.region_invalid_radius"))

            SetRegionResult.NotFound ->
                notice(MessageKey("movement.random.region_not_found"), identifier(name))

            SetRegionResult.WrongDimension ->
                notice(MessageKey("movement.random.region_wrong_dimension"), identifier(name))

            SetRegionResult.LimitReached ->
                notice(MessageKey("movement.random.region_limit"))
        }

    fun randomRegionRemoved(name: String, result: DeleteRegionResult): Notice =
        when (result) {
            DeleteRegionResult.Deleted ->
                success(MessageKey("movement.random.region_removed"), identifier(name))

            DeleteRegionResult.InvalidName ->
                notice(MessageKey("movement.random.region_invalid_name"))

            DeleteRegionResult.NotFound ->
                notice(
                    MessageKey("movement.random.region_not_found"),
                    identifier(name),
                )
        }

    fun randomRegionLookupError(result: RegionLookupResult): Notice? =
        when (result) {
            is RegionLookupResult.Found ->
                null

            RegionLookupResult.InvalidName ->
                notice(MessageKey("movement.random.region_invalid_name"))

            RegionLookupResult.NotFound ->
                notice(MessageKey("movement.random.region_not_found"))
        }

    fun randomRegions(regions: List<RandomTeleportRegion>): ListPage =
        ListPage(
            title = LocalizedText(MessageKey("movement.random.list_title")),
            rows = regions.map { region ->
                ListRow(
                    main = LocalizedText(
                        MessageKey("core.value"),
                        listOf(identifier(region.name.value)),
                    ),
                    supporting = LocalizedText(
                        MessageKey("core.value"),
                        listOf(literal(region.dimension)),
                    ),
                )
            },
            emptyText = LocalizedText(MessageKey("movement.random.list_empty")),
        )

    fun randomRegionInfo(result: RegionLookupResult.Found): DetailView {
        val region = result.region
        val center = "${floor(region.centerX).toInt()}, ${floor(region.centerZ).toInt()}"
        val range = "${region.minimumRadius.toInt()} - ${region.maximumRadius.toInt()}"
        return DetailView(
            title = LocalizedText(
                MessageKey("movement.random.info.title"),
                listOf(identifier(region.name.value)),
            ),
            fields = listOf(
                DetailField(
                    label = LocalizedText(MessageKey("movement.random.info.field.dimension")),
                    value = LocalizedText(
                        MessageKey("core.value"),
                        listOf(literal(region.dimension)),
                    ),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("movement.random.info.field.center")),
                    value = LocalizedText(
                        MessageKey("core.value"),
                        listOf(literal(center)),
                    ),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("movement.random.info.field.range")),
                    value = LocalizedText(
                        MessageKey("core.value"),
                        listOf(literal(range)),
                    ),
                ),
                DetailField(
                    label = LocalizedText(MessageKey("movement.random.info.field.status")),
                    value = LocalizedText(
                        if (region.enabled) {
                            MessageKey("movement.random.info.enabled")
                        } else {
                            MessageKey("movement.random.info.disabled")
                        }
                    ),
                ),
            ),
        )
    }

    fun terrainTeleported(target: TerrainTarget): Notice =
        when (target) {
            TerrainTarget.TOP -> success(MessageKey("movement.terrain.topped"))
            TerrainTarget.BOTTOM -> success(MessageKey("movement.terrain.bottomed"))
            TerrainTarget.JUMP -> success(MessageKey("movement.terrain.jumped"))
        }

    fun terrainError(result: TerrainResult): Notice? =
        when (result) {
            is TerrainResult.Found ->
                null

            TerrainResult.Disabled ->
                notice(MessageKey("movement.terrain.disabled"))

            TerrainResult.NoSafeSpot ->
                notice(MessageKey("movement.terrain.no_safe_spot"))

            TerrainResult.ChunkNotLoaded ->
                notice(MessageKey("movement.terrain.chunk_not_loaded"))

            TerrainResult.NoTarget ->
                notice(MessageKey("movement.terrain.no_target"))

            TerrainResult.PlayerOffline ->
                notice(MessageKey("movement.teleport.error.player_offline"))
        }

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

    private fun literal(value: String): TextArgument =
        TextArgument.Literal(value)

    private fun playerName(value: String): TextArgument =
        TextArgument.PlayerName(value)

    private fun elapsed(value: Duration): TextArgument =
        TextArgument.Elapsed(value)

    private fun count(value: Long): TextArgument =
        TextArgument.Count(value)

    private fun blockPosition(position: StoredPosition): TextArgument =
        TextArgument.Position(
            x = floor(position.x).toInt(),
            y = floor(position.y).toInt(),
            z = floor(position.z).toInt(),
            dimension = position.dimension,
        )

    private fun ago(from: Instant, now: Instant): Duration =
        (now.toEpochMilli() - from.toEpochMilli()).coerceAtLeast(0L).milliseconds

}
