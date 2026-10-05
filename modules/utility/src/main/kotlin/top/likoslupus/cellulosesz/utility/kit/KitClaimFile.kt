package top.likoslupus.cellulosesz.utility.kit

import kotlinx.serialization.Serializable

internal const val KIT_CLAIMS_SCHEMA_VERSION: Int = 1
internal const val MAX_STORED_CLAIMS_PER_PLAYER: Int = 10_000

@Serializable
internal data class KitClaimsFile(
    val schemaVersion: Int = KIT_CLAIMS_SCHEMA_VERSION,
    val playerId: String,
    val playerName: String,
    val claims: Map<String, KitClaimRecord> = emptyMap(),
)

@Serializable
internal data class KitClaimRecord(
    val kitId: String,
    val claimedAtEpochMillis: Long,
    val status: KitClaimStatus,
)
