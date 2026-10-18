package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.movement.random.*
import top.likoslupus.cellulosesz.movement.terrain.TerrainResult
import top.likoslupus.cellulosesz.movement.terrain.TerrainTarget
import kotlin.math.floor

internal fun MovementMessages.randomTeleported(): Notice =
    success(MessageKey("movement.random.teleported"))

internal fun MovementMessages.randomTeleportError(result: RandomTeleportResult): Notice? =
    when (result) {
        is RandomTeleportResult.Found ->
            null

        RandomTeleportResult.Disabled ->
            notice(MessageKey("movement.random.disabled"))

        RandomTeleportResult.NotConfigured ->
            notice(MessageKey("movement.random.not_configured"))

        is RandomTeleportResult.UnknownRegion ->
            notice(
                MessageKey("movement.random.unknown_region"),
                identifier(result.name)
            )

        is RandomTeleportResult.WrongDimension ->
            notice(
                MessageKey("movement.random.wrong_dimension"),
                identifier(result.name)
            )

        is RandomTeleportResult.RegionDisabled ->
            notice(
                MessageKey("movement.random.region_disabled"),
                identifier(result.name)
            )

        is RandomTeleportResult.NoValidArea ->
            notice(
                MessageKey("movement.random.no_area"),
                identifier(result.name)
            )

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

internal fun MovementMessages.randomRegionSet(name: String, result: SetRegionResult): Notice =
    when (result) {
        SetRegionResult.Saved ->
            success(
                MessageKey("movement.random.region_set"),
                identifier(name)
            )

        SetRegionResult.InvalidName ->
            notice(MessageKey("movement.random.region_invalid_name"))

        SetRegionResult.InvalidRange ->
            notice(MessageKey("movement.random.region_invalid_radius"))

        SetRegionResult.NotFound ->
            notice(
                MessageKey("movement.random.region_not_found"),
                identifier(name)
            )

        SetRegionResult.WrongDimension ->
            notice(
                MessageKey("movement.random.region_wrong_dimension"),
                identifier(name)
            )

        SetRegionResult.LimitReached ->
            notice(MessageKey("movement.random.region_limit"))
    }

internal fun MovementMessages.randomRegionRemoved(
    name: String,
    result: DeleteRegionResult
): Notice =
    when (result) {
        DeleteRegionResult.Deleted ->
            success(
                MessageKey("movement.random.region_removed"),
                identifier(name)
            )

        DeleteRegionResult.InvalidName ->
            notice(MessageKey("movement.random.region_invalid_name"))

        DeleteRegionResult.NotFound ->
            notice(
                MessageKey("movement.random.region_not_found"),
                identifier(name),
            )
    }

internal fun MovementMessages.randomRegionLookupError(result: RegionLookupResult): Notice? =
    when (result) {
        is RegionLookupResult.Found ->
            null

        RegionLookupResult.InvalidName ->
            notice(MessageKey("movement.random.region_invalid_name"))

        RegionLookupResult.NotFound ->
            notice(MessageKey("movement.random.region_not_found"))
    }

internal fun MovementMessages.randomRegions(regions: List<RandomTeleportRegion>): ListPage =
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

internal fun MovementMessages.randomRegionInfo(result: RegionLookupResult.Found): DetailView {
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

internal fun MovementMessages.terrainTeleported(target: TerrainTarget): Notice =
    when (target) {
        TerrainTarget.TOP -> success(MessageKey("movement.terrain.topped"))
        TerrainTarget.BOTTOM -> success(MessageKey("movement.terrain.bottomed"))
        TerrainTarget.JUMP -> success(MessageKey("movement.terrain.jumped"))
    }

internal fun MovementMessages.terrainError(result: TerrainResult): Notice? =
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
