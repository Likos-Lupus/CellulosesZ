package top.likoslupus.cellulosesz.administration.moderation.mute

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.administration.moderation.audit.PersistedModerationActor
import java.time.Instant
import java.util.*

internal const val MUTE_SCHEMA_VERSION: Int = 1

@Serializable
internal data class MuteFile(
    val schemaVersion: Int = MUTE_SCHEMA_VERSION,
    val playerId: String,
    val playerName: String,
    val actor: PersistedModerationActor,
    val reason: String,
    val issuedAtEpochMillis: Long,
    val expiresAtEpochMillis: Long? = null,
)

internal fun Mute.toFile(): MuteFile =
    MuteFile(
        playerId = playerId.toString(),
        playerName = playerName,
        actor = actor,
        reason = reason,
        issuedAtEpochMillis = issuedAt.toEpochMilli(),
        expiresAtEpochMillis = expiresAt?.toEpochMilli(),
    )

internal fun MuteFile.toDomain(): Mute =
    Mute(
        playerId = UUID.fromString(playerId),
        playerName = playerName,
        actor = actor,
        reason = reason,
        issuedAt = Instant.ofEpochMilli(issuedAtEpochMillis),
        expiresAt = expiresAtEpochMillis?.let(Instant::ofEpochMilli),
    )
