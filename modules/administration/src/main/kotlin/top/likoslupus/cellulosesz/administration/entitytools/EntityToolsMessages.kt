package top.likoslupus.cellulosesz.administration.entitytools

import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.presentation.MessageSupport
import java.util.*

/** Semantic results for `/spawner`, `/spawnmob` and `/remove`. */
internal object EntityToolsMessages : MessageSupport() {

    fun spawnerConfigured(type: String, delayTicks: Int?): Notice =
        when (delayTicks) {
            null -> success(
                MessageKey("administration.spawner.success"),
                identifier(type),
            )

            else -> success(
                MessageKey("administration.spawner.delay"),
                identifier(type),
                count(delayTicks),
            )
        }

    fun notASpawner(): Notice =
        error(MessageKey("administration.spawner.not_spawner"))

    fun spawnerForbidden(type: String): Notice =
        error(
            MessageKey("administration.spawner.forbidden"),
            identifier(type),
        )

    fun spawnMobSpawned(type: String, count: Int): Notice =
        success(
            MessageKey("administration.spawnmob.success"),
            count(count),
            identifier(type),
        )

    fun spawnMobCapped(requested: Int, spawned: Int): Notice =
        success(
            MessageKey("administration.spawnmob.capped"),
            count(requested),
            count(spawned),
        )

    fun spawnMobStackForbidden(): Notice =
        error(MessageKey("administration.spawnmob.stack_forbidden"))

    fun removeSuccess(selector: String, count: Int): Notice =
        success(
            MessageKey("administration.remove.success"),
            count(count),
            literal(selector),
        )

    fun removeInvalid(): Notice =
        error(MessageKey("administration.remove.invalid"))

    fun unknownEntityType(raw: String): Notice =
        error(
            MessageKey("administration.entity_type.unknown"),
            literal(raw),
        )

    fun radiusTooLarge(max: Double): Notice =
        error(
            MessageKey("administration.remove.radius_too_large"),
            literal(format(max)),
        )

    fun invalidRadius(): Notice =
        error(MessageKey("administration.remove.invalid_radius"))

    fun playerNotFound(): Notice =
        error(MessageKey("administration.playerstate.player_not_found"))

    fun lookAtBlock(): Notice =
        error(MessageKey("administration.tools.look_at_block"))

    private fun format(value: Double): String =
        if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.ROOT, "%.1f", value)
        }

}
