package top.likoslupus.cellulosesz.administration.moderation.audit

/**
 * Append-only moderation audit sink. The interface expresses domain intent; the file implementation
 * chooses the storage topology (one immutable record per file).
 */
internal interface ModerationAuditRepository {

    suspend fun append(entry: ModerationAuditFile)

}
