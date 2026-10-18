package top.likoslupus.cellulosesz.utility.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.utility.inspection.InspectionOpenResult
import top.likoslupus.cellulosesz.utility.position.DepthReading
import top.likoslupus.cellulosesz.utility.workstation.WorkstationResult

internal fun UtilityMessages.positionView(
    dimension: String,
    x: Int,
    y: Int,
    z: Int,
): DetailView =
    DetailView(
        title = LocalizedText(MessageKey("utility.getpos.title")),
        fields = listOf(
            field(
                MessageKey("utility.getpos.field.position"),
                LocalizedText(
                    MessageKey("core.value"),
                    listOf(
                        position(
                            x,
                            y,
                            z,
                            dimension
                        )
                    ),
                ),
            )
        ),
    )

internal fun UtilityMessages.depth(reading: DepthReading): DetailView {
    val depthValue = when {
        reading.belowAnchor > 0 ->
            LocalizedText(
                MessageKey("utility.depth.below"),
                listOf(
                    count(reading.belowAnchor),
                    count(reading.anchor),
                ),
            )

        reading.aboveAnchor > 0 ->
            LocalizedText(
                MessageKey("utility.depth.above"),
                listOf(
                    count(reading.aboveAnchor),
                    count(reading.anchor),
                ),
            )

        else -> LocalizedText(
            MessageKey("utility.depth.at"),
            listOf(count(reading.anchor))
        )
    }
    return DetailView(
        title = LocalizedText(MessageKey("utility.depth.title")),
        fields = listOf(
            field(MessageKey("utility.depth.field.depth"), depthValue),
            field(
                MessageKey("utility.depth.field.range"),
                LocalizedText(
                    MessageKey("utility.depth.range"),
                    listOf(
                        count(reading.minY),
                        count(reading.maxY),
                    ),
                ),
            ),
        ),
    )
}

internal fun UtilityMessages.workstation(result: WorkstationResult): Notice =
    notice(
        when (result) {
            WorkstationResult.Opened -> return info(MessageKey("utility.workstation.opened"))
            WorkstationResult.Disabled -> MessageKey("utility.workstation.disabled")
            WorkstationResult.NotAllowed -> MessageKey("utility.workstation.not_allowed")
            WorkstationResult.PlayerOffline -> MessageKey("utility.kit.player_offline")
            WorkstationResult.Failed -> MessageKey("utility.workstation.failed")
        }
    )

internal fun UtilityMessages.inspection(result: InspectionOpenResult): Notice =
    notice(
        when (result) {
            InspectionOpenResult.Opened -> return info(MessageKey("utility.inspection.opened"))
            InspectionOpenResult.PlayerOffline -> MessageKey("utility.item.target_offline")
            InspectionOpenResult.Failed -> MessageKey("utility.inspection.failed")
        }
    )

internal fun UtilityMessages.inspectionSelf(): Notice =
    notice(MessageKey("utility.inspection.self"))
