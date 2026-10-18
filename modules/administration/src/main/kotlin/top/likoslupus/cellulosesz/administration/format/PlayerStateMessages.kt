package top.likoslupus.cellulosesz.administration.format

import top.likoslupus.cellulosesz.administration.playerstate.*
import top.likoslupus.cellulosesz.core.text.MessageKey
import top.likoslupus.cellulosesz.core.text.model.DetailView
import top.likoslupus.cellulosesz.core.text.model.LocalizedText
import top.likoslupus.cellulosesz.core.text.model.Notice
import java.util.*

internal fun AdministrationMessages.healed(target: String): Notice =
    success(
        MessageKey("administration.playerstate.healed"),
        player(target)
    )

internal fun AdministrationMessages.heal(result: HealResult): Notice =
    when (result) {
        is HealResult.Healed -> success(
            MessageKey("administration.playerstate.healed"),
            player(result.target),
        )

        is HealResult.OnCooldown -> error(
            MessageKey("administration.playerstate.heal_cooldown"),
            elapsed(result.remaining),
        )

        HealResult.TargetOffline -> error(
            MessageKey("administration.playerstate.player_not_found")
        )
    }

internal fun AdministrationMessages.fed(target: String): Notice =
    success(
        MessageKey("administration.playerstate.fed"),
        player(target)
    )

internal fun AdministrationMessages.flight(target: String, enabled: Boolean): Notice =
    success(
        when {
            enabled -> MessageKey("administration.playerstate.flight_enabled")
            else -> MessageKey("administration.playerstate.flight_disabled")
        },
        player(target),
    )

internal fun AdministrationMessages.flight(result: FlyResult): Notice =
    when (result) {
        is FlyResult.Applied -> success(
            when {
                result.enabled -> MessageKey("administration.playerstate.flight_enabled")
                else -> MessageKey("administration.playerstate.flight_disabled")
            },
            player(result.target),
        )

        FlyResult.TargetOffline -> error(
            MessageKey("administration.playerstate.player_not_found")
        )
    }

internal fun AdministrationMessages.god(target: String, enabled: Boolean): Notice =
    success(
        when {
            enabled -> MessageKey("administration.playerstate.god_enabled")
            else -> MessageKey("administration.playerstate.god_disabled")
        },
        player(target),
    )

internal fun AdministrationMessages.god(result: GodResult): Notice =
    when (result) {
        is GodResult.Applied -> success(
            when {
                result.enabled -> MessageKey("administration.playerstate.god_enabled")
                else -> MessageKey("administration.playerstate.god_disabled")
            },
            player(result.target),
        )

        GodResult.DisabledWorld -> error(
            MessageKey("administration.playerstate.god_disabled_world")
        )

        GodResult.TargetOffline -> error(
            MessageKey("administration.playerstate.player_not_found")
        )
    }

internal fun AdministrationMessages.playerNotFound(): Notice =
    error(MessageKey("administration.playerstate.player_not_found"))

internal fun AdministrationMessages.notFound(): Notice =
    error(MessageKey("administration.playerstate.player_not_found"))

internal fun AdministrationMessages.experienceView(result: ExperienceResult.Viewed): DetailView =
    DetailView(
        title = LocalizedText(MessageKey("administration.exp.title")),
        fields = listOf(
            field(
                MessageKey("administration.exp.field.target"),
                bare(player(result.target))
            ),
            field(
                MessageKey("administration.exp.field.level"),
                bare(count(result.level))
            ),
            field(
                MessageKey("administration.exp.field.total"),
                bare(count(result.total))
            ),
            field(
                MessageKey("administration.exp.field.needed"),
                bare(count(result.needed))
            ),
        ),
    )

internal fun AdministrationMessages.experienceChange(result: ExperienceResult): Notice =
    when (result) {
        is ExperienceResult.Changed ->
            success(
                when (result.operation) {
                    ExperienceOperation.GIVE -> MessageKey("administration.exp.give")
                    ExperienceOperation.TAKE -> MessageKey("administration.exp.take")
                    ExperienceOperation.SET -> MessageKey("administration.exp.set")
                },
                count(result.amount),
                player(result.target),
                count(result.level),
            )

        ExperienceResult.TargetOffline ->
            error(MessageKey("administration.playerstate.player_not_found"))

        is ExperienceResult.Viewed ->
            error(MessageKey("administration.error.unexpected"))
    }

internal fun AdministrationMessages.speed(result: SpeedResult): Notice =
    when (result) {
        is SpeedResult.Applied ->
            success(
                MessageKey("administration.speed.applied"),
                literal(multiplier(result.multiplier)),
                player(result.target),
            )

        SpeedResult.TargetOffline ->
            error(MessageKey("administration.playerstate.player_not_found"))
    }

private fun AdministrationMessages.multiplier(value: Double): String =
    when {
        value == value.toLong().toDouble() -> value.toLong().toString()
        else -> String.format(Locale.ROOT, "%.2f", value)
    }
