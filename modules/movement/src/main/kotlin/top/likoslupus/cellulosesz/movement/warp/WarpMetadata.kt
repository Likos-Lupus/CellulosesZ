package top.likoslupus.cellulosesz.movement.warp

import java.time.Instant

/** Sidecar bookkeeping for a warp: who created it, when, and when it was last updated. */
internal data class WarpMetadata(
    val createdBy: String,
    val createdAt: Instant,
    val updatedAt: Instant?,
)

/**
 * Read access to warp metadata. Writes are owned by [WarpRepository] so the warp row and its sidecar
 * row always change in the same transaction.
 */
internal interface WarpMetadataRepository {

    suspend fun metadata(name: WarpName): WarpMetadata?

}
