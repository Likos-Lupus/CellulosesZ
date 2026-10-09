package top.likoslupus.cellulosesz.administration.moderation.audit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.administration.AdministrationSqlSchema
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.SQLException
import java.sql.Types

/** Append-only audit in `cz_moderation_audit` + `cz_moderation_audit_details` (no JSON blob). */
internal class JdbcModerationAuditRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : ModerationAuditRepository {

    override suspend fun append(entry: ModerationAuditFile) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${AdministrationSqlSchema.MODERATION_AUDIT} (
                            namespace,
                            audit_id,
                            occurred_at_ms,
                            actor_type,
                            actor_uuid,
                            actor_name,
                            action,
                            target_type,
                            target_id,
                            target_name,
                            reason,
                            expires_at_ms
                        )
                        VALUES (
                            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, entry.id)
                        statement.setLong(3, entry.occurredAtEpochMillis)
                        statement.setString(4, entry.actor.type)
                        when (entry.actor.id) {
                            null -> statement.setNull(5, Types.VARCHAR)
                            else -> statement.setString(5, entry.actor.id)
                        }
                        statement.setString(6, entry.actor.name)
                        statement.setString(7, entry.action.name)
                        statement.setString(8, entry.target.type)
                        when (entry.target.id) {
                            null -> statement.setNull(9, Types.VARCHAR)
                            else -> statement.setString(9, entry.target.id)
                        }
                        statement.setString(10, entry.target.name)
                        when (entry.reason) {
                            null -> statement.setNull(11, Types.VARCHAR)
                            else -> statement.setString(11, entry.reason)
                        }
                        when (entry.expiresAtEpochMillis) {
                            null -> statement.setNull(12, Types.BIGINT)
                            else -> statement.setLong(12, entry.expiresAtEpochMillis)
                        }
                        statement.executeUpdate()
                    }

                    if (entry.details.isNotEmpty()) {
                        connection.prepareStatement(
                            /* language=SQL */ """
                            INSERT INTO ${AdministrationSqlSchema.MODERATION_AUDIT_DETAILS} (
                                namespace,
                                audit_id,
                                detail_key,
                                detail_value
                            )
                            VALUES (
                                ?, ?, ?, ?
                            );
                            """.trimIndent()
                        ).use { statement ->
                            entry.details.forEach { (key, value) ->
                                statement.setString(1, namespace)
                                statement.setString(2, entry.id)
                                statement.setString(3, key)
                                statement.setString(4, value)
                                statement.addBatch()
                            }
                            statement.executeBatch()
                        }
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw AuditDataException("unable to append audit record ${entry.id}", exception)
            }
        }
}

/** Raised when an audit record cannot be persisted. */
internal class AuditDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
