package top.likoslupus.cellulosesz.movement.request

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.movement.MovementSqlSchema
import java.sql.SQLException
import java.util.*

internal class JdbcTpAutoRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : TpAutoRepository {

    override fun loadAllBlocking(): TpAutoSnapshot =
        try {
            database.read { connection ->
                val modes = LinkedHashMap<UUID, TpAutoMode>()
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        mode
                    FROM
                        ${MovementSqlSchema.TPAUTO_PREFERENCES}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        while (rs.next()) {
                            modes[parseUuid(rs.getString(1))] = parseMode(rs.getString(2))
                        }
                    }
                }

                val allowed = LinkedHashMap<UUID, MutableSet<UUID>>()
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        owner_uuid,
                        allowed_uuid
                    FROM
                        ${MovementSqlSchema.TPAUTO_ALLOWED}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        while (rs.next()) {
                            allowed.getOrPut(parseUuid(rs.getString(1))) { mutableSetOf() }
                                    .add(parseUuid(rs.getString(2)))
                        }
                    }
                }

                TpAutoSnapshot(modes, allowed)
            }
        } catch (exception: SQLException) {
            throw TpAutoDataException("unable to read auto-accept preferences", exception)
        }

    override suspend fun setMode(owner: UUID, mode: TpAutoMode) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${MovementSqlSchema.TPAUTO_PREFERENCES}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, owner.toString())
                        statement.executeUpdate()
                    }
                    if (mode != TpAutoMode.OFF) {
                        connection.prepareStatement(
                            /* language=SQL */ """
                            INSERT INTO ${MovementSqlSchema.TPAUTO_PREFERENCES} (
                                namespace,
                                player_uuid,
                                mode
                            )
                            VALUES (
                                ?, ?, ?
                            );
                            """.trimIndent()
                        ).use { statement ->
                            statement.setString(1, namespace)
                            statement.setString(2, owner.toString())
                            statement.setString(3, mode.id)
                            statement.executeUpdate()
                        }
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw TpAutoDataException(
                    "unable to write auto-accept preference for $owner",
                    exception
                )
            }
        }

    override suspend fun addAllowed(owner: UUID, allowed: UUID) =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${MovementSqlSchema.TPAUTO_ALLOWED} (
                            namespace,
                            owner_uuid,
                            allowed_uuid
                        )
                        VALUES (
                            ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, owner.toString())
                        statement.setString(3, allowed.toString())
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw TpAutoDataException(
                    "unable to allow $allowed for auto-accept owner $owner",
                    exception,
                )
            }
        }

    override suspend fun removeAllowed(owner: UUID, allowed: UUID): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${MovementSqlSchema.TPAUTO_ALLOWED}
                        WHERE
                            namespace = ? AND owner_uuid = ? AND allowed_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, owner.toString())
                        statement.setString(3, allowed.toString())
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw TpAutoDataException(
                    "unable to remove $allowed from auto-accept owner $owner",
                    exception,
                )
            }
        }

    private fun parseUuid(raw: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (exception: IllegalArgumentException) {
            throw TpAutoDataException("corrupt player UUID '$raw' in auto-accept state", exception)
        }

    private fun parseMode(raw: String): TpAutoMode =
        TpAutoMode.parse(raw)
            ?: throw TpAutoDataException("corrupt auto-accept mode '$raw'")

}
