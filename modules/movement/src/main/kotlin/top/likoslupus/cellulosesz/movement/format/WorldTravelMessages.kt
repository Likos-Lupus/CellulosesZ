package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.*
import top.likoslupus.cellulosesz.movement.compass.CompassPoint
import top.likoslupus.cellulosesz.movement.compass.CompassReading
import top.likoslupus.cellulosesz.movement.world.WorldEntry

internal fun MovementMessages.currentWorld(alias: String?, dimension: String): DetailView =
    DetailView(
        title = LocalizedText(MessageKey("movement.world.current_title")),
        fields = listOf(
            DetailField(
                label = LocalizedText(MessageKey("movement.world.field.name")),
                value = LocalizedText(
                    MessageKey("core.value"),
                    listOf(literal(alias ?: dimension)),
                ),
            ),
            DetailField(
                label = LocalizedText(MessageKey("movement.world.field.dimension")),
                value = LocalizedText(
                    MessageKey("core.value"),
                    listOf(literal(dimension)),
                ),
            ),
        ),
    )

internal fun MovementMessages.worlds(entries: List<WorldEntry>): ListPage =
    ListPage(
        title = LocalizedText(MessageKey("movement.world.list_title")),
        rows = entries.map { entry ->
            ListRow(
                main = LocalizedText(
                    MessageKey("core.value"),
                    listOf(literal(entry.alias ?: entry.dimension)),
                ),
                supporting = entry.alias
                        ?.let {
                            LocalizedText(
                                MessageKey("movement.world.list_dimension"),
                                listOf(identifier(entry.dimension)),
                            )
                        },
            )
        },
        emptyText = LocalizedText(MessageKey("movement.world.list_empty")),
    )

internal fun MovementMessages.unknownWorld(name: String): Notice =
    notice(
        MessageKey("movement.world.unknown"),
        literal(name),
    )

internal fun MovementMessages.teleportedToWorld(name: String): Notice =
    success(
        MessageKey("movement.world.teleported"),
        identifier(name),
    )

internal fun MovementMessages.compass(reading: CompassReading): DetailView =
    DetailView(
        title = LocalizedText(MessageKey("movement.compass.title")),
        fields = listOf(
            DetailField(
                label = LocalizedText(MessageKey("movement.compass.field.direction")),
                value = LocalizedText(compassDirection(reading.point)),
            ),
            DetailField(
                label = LocalizedText(MessageKey("movement.compass.field.bearing")),
                value = LocalizedText(
                    MessageKey("movement.compass.bearing"),
                    listOf(count(reading.bearingDegrees.toLong())),
                ),
            ),
        ),
    )

internal fun MovementMessages.teleportedToPlayer(name: String): Notice =
    success(
        MessageKey("movement.teleport.to_player"),
        playerName(name),
    )

internal fun MovementMessages.teleportedOther(subject: String, destination: String): Notice =
    success(
        MessageKey("movement.teleport.other_to_other"),
        playerName(subject),
        playerName(destination),
    )

internal fun MovementMessages.teleportedHere(name: String): Notice =
    success(
        MessageKey("movement.teleport.here"),
        playerName(name),
    )

internal fun MovementMessages.teleportedToYou(name: String): Notice =
    info(
        MessageKey("movement.teleport.here_notify"),
        playerName(name),
    )

internal fun MovementMessages.teleportedToOffline(name: String): Notice =
    success(
        MessageKey("movement.teleport.offline"),
        playerName(name),
    )

internal fun MovementMessages.noLastLocation(name: String): Notice =
    notice(
        MessageKey("movement.teleport.no_last_location"),
        playerName(name),
    )

internal fun MovementMessages.teleportedAll(count: Int, target: String?): Notice =
    when (target) {
        null -> success(
            MessageKey("movement.teleport.all_here"),
            count(count.toLong()),
        )

        else -> success(
            MessageKey("movement.teleport.all"),
            count(count.toLong()),
            playerName(target),
        )
    }

private fun compassDirection(point: CompassPoint): MessageKey =
    when (point) {
        CompassPoint.NORTH -> MessageKey("movement.compass.direction.north")
        CompassPoint.NORTH_EAST -> MessageKey("movement.compass.direction.north_east")
        CompassPoint.EAST -> MessageKey("movement.compass.direction.east")
        CompassPoint.SOUTH_EAST -> MessageKey("movement.compass.direction.south_east")
        CompassPoint.SOUTH -> MessageKey("movement.compass.direction.south")
        CompassPoint.SOUTH_WEST -> MessageKey("movement.compass.direction.south_west")
        CompassPoint.WEST -> MessageKey("movement.compass.direction.west")
        CompassPoint.NORTH_WEST -> MessageKey("movement.compass.direction.north_west")
    }
