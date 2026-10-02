package top.likoslupus.cellulosesz.administration.moderation.audit

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.administration.moderation.ModerationActor
import top.likoslupus.cellulosesz.administration.moderation.PlayerIdentity

internal const val AUDIT_SCHEMA_VERSION: Int = 1

/** Actions recorded in the structured moderation audit log. */
@Serializable
internal enum class ModerationAuditAction {

    KICK,
    KICK_ALL,
    BAN,
    TEMP_BAN,
    UNBAN,
    BAN_IP,
    TEMP_BAN_IP,
    UNBAN_IP,
    MUTE,
    TEMP_MUTE,
    UNMUTE,
    KILL,
    GAMEMODE,
    SUDO,
    SOCIAL_SPY_ENABLE,
    SOCIAL_SPY_DISABLE,
    VANISH_ENABLE,
    VANISH_DISABLE,

}

/** Persisted form of a moderation actor (console has no id). */
@Serializable
internal data class PersistedModerationActor(
    val type: String,
    val id: String? = null,
    val name: String,
) {

    companion object {

        fun of(actor: ModerationActor): PersistedModerationActor =
            when (actor) {
                ModerationActor.Console ->
                    PersistedModerationActor(
                        type = "console",
                        id = null,
                        name = "console"
                    )

                is ModerationActor.Player ->
                    PersistedModerationActor(
                        type = "player",
                        id = actor.id.toString(),
                        name = actor.name
                    )
            }

    }

}

/** Persisted form of a moderation target (account or IP). */
@Serializable
internal data class PersistedModerationTarget(
    val type: String,
    val id: String? = null,
    val name: String,
) {

    companion object {

        fun account(identity: PlayerIdentity): PersistedModerationTarget =
            PersistedModerationTarget(
                type = "account",
                id = identity.id.toString(),
                name = identity.name
            )

        fun ip(address: String): PersistedModerationTarget =
            PersistedModerationTarget(
                type = "ip",
                id = address,
                name = address
            )

    }

}

/** One immutable moderation audit record. */
@Serializable
internal data class ModerationAuditFile(
    val schemaVersion: Int = AUDIT_SCHEMA_VERSION,
    val id: String,
    val occurredAtEpochMillis: Long,
    val actor: PersistedModerationActor,
    val action: ModerationAuditAction,
    val target: PersistedModerationTarget,
    val reason: String? = null,
    val expiresAtEpochMillis: Long? = null,
    val details: Map<String, String> = emptyMap(),
)
