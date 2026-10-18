package top.likoslupus.cellulosesz.movement.format

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.movement.spawn.ClearSpawnResult

internal fun MovementMessages.teleportToSpawn(): Notice =
    success(MessageKey("movement.spawn.teleported"))

internal fun MovementMessages.spawnSet(): Notice =
    success(MessageKey("movement.spawn.set"))

internal fun MovementMessages.spawnClear(result: ClearSpawnResult): Notice =
    when (result) {
        ClearSpawnResult.Cleared ->
            success(MessageKey("movement.spawn.cleared"))

        ClearSpawnResult.NotConfigured ->
            notice(MessageKey("movement.spawn.not_configured"))
    }
