package top.likoslupus.cellulosesz.administration.entitytools

import top.likoslupus.cellulosesz.administration.format.AdminText
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import top.likoslupus.cellulosesz.core.text.model.NoticeTone
import top.likoslupus.cellulosesz.core.text.model.TextArgument

/** Semantic results for `/spawner`, `/spawnmob` and `/remove`. */
internal object EntityToolsMessages {

    fun spawnerConfigured(type: String, delayTicks: Int?): Notice =
        when (delayTicks) {
            null -> success(
                MessageKey("administration.spawner.success"),
                AdminText.identifier(type),
            )

            else -> success(
                MessageKey("administration.spawner.delay"),
                AdminText.identifier(type),
                AdminText.count(delayTicks),
            )
        }

    fun notASpawner(): Notice =
        error(MessageKey("administration.spawner.not_spawner"))

    fun spawnMobSpawned(type: String, count: Int): Notice =
        success(
            MessageKey("administration.spawnmob.success"),
            AdminText.count(count),
            AdminText.identifier(type),
        )

    fun spawnMobCapped(requested: Int, spawned: Int): Notice =
        success(
            MessageKey("administration.spawnmob.capped"),
            AdminText.count(requested),
            AdminText.count(spawned),
        )

    fun removeSuccess(selector: String, count: Int): Notice =
        success(
            MessageKey("administration.remove.success"),
            AdminText.count(count),
            AdminText.literal(selector),
        )

    fun removeInvalid(): Notice =
        error(MessageKey("administration.remove.invalid"))

    fun unknownEntityType(raw: String): Notice =
        error(
            MessageKey("administration.entity_type.unknown"),
            AdminText.literal(raw),
        )

    fun radiusTooLarge(max: Double): Notice =
        error(
            MessageKey("administration.remove.radius_too_large"),
            AdminText.literal(format(max)),
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
            String.format(java.util.Locale.ROOT, "%.1f", value)
        }

    private fun success(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.SUCCESS
        )

    private fun error(key: MessageKey, vararg arguments: TextArgument): Notice =
        Notice(
            LocalizedText(key, arguments.toList()),
            NoticeTone.ERROR
        )

}
