package top.likoslupus.cellulosesz.utility.kit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.likoslupus.cellulosesz.core.player.KnownPlayerIdentity
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant
import java.util.*

/** Per-player kit claims in `cz_kit_claims`; RESERVED is written before delivery (fail closed). */
internal class JdbcKitClaimRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : KitClaimRepository {

    override suspend fun load(playerId: UUID): Map<KitName, KitClaim> =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            kit_name,
                            kit_id,
                            claimed_at_ms,
                            status
                        FROM
                            ${UtilitySqlSchema.KIT_CLAIMS}
                        WHERE
                            namespace = ? AND player_uuid = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.executeQuery().use { rs ->
                            val claims = LinkedHashMap<KitName, KitClaim>()
                            while (rs.next()) {
                                val name = KitName.parse(rs.getString(1))
                                    ?: throw KitClaimDataException("kit claims contain an invalid kit name")
                                claims[name] = KitClaim(
                                    kitId = parseUuid(rs.getString(2), "kit id"),
                                    claimedAt = Instant.ofEpochMilli(rs.getLong(3)),
                                    status = parseStatus(rs.getString(4)),
                                )
                            }
                            if (claims.size > MAX_STORED_CLAIMS_PER_PLAYER) {
                                throw KitClaimDataException("kit claims exceed the stored cap for $playerId")
                            }
                            claims
                        }
                    }
                }
            } catch (exception: SQLException) {
                throw KitClaimDataException("unable to read kit claims for $playerId", exception)
            }
        }

    override suspend fun reserve(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant
    ) =
        upsert(
            player,
            kit,
            at,
            KitClaimStatus.RESERVED
        )

    override suspend fun markDelivered(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant
    ) =
        upsert(
            player,
            kit,
            at,
            KitClaimStatus.DELIVERED
        )

    override suspend fun reset(playerId: UUID, name: KitName): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${UtilitySqlSchema.KIT_CLAIMS}
                        WHERE
                            namespace = ? AND player_uuid = ? AND kit_name = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.setString(3, name.value)
                        statement.executeUpdate() > 0
                    }
                }
            } catch (exception: SQLException) {
                throw KitClaimDataException("unable to reset kit claim for $playerId", exception)
            }
        }

    private suspend fun upsert(
        player: KnownPlayerIdentity,
        kit: KitDefinition,
        at: Instant,
        status: KitClaimStatus,
    ) = withContext(Dispatchers.IO) {
        try {
            database.transaction { connection ->
                val count = countClaims(connection, player.id)
                if (count >= MAX_STORED_CLAIMS_PER_PLAYER
                    && !hasClaim(
                        connection,
                        player.id,
                        kit.name
                    )
                ) {
                    throw KitClaimDataException("claim history exceeds the stored cap for ${player.id}")
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    DELETE FROM
                        ${UtilitySqlSchema.KIT_CLAIMS}
                    WHERE
                        namespace = ? AND player_uuid = ? AND kit_name = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, player.id.toString())
                    statement.setString(3, kit.name.value)
                    statement.executeUpdate()
                }
                connection.prepareStatement(
                    /* language=SQL */ """
                    INSERT INTO ${UtilitySqlSchema.KIT_CLAIMS} (
                        namespace,
                        player_uuid,
                        player_name,
                        kit_name,
                        kit_id,
                        claimed_at_ms,
                        status
                    )
                    VALUES (
                        ?, ?, ?, ?, ?, ?, ?
                    );
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.setString(2, player.id.toString())
                    statement.setString(3, player.name)
                    statement.setString(4, kit.name.value)
                    statement.setString(5, kit.id.value.toString())
                    statement.setLong(6, at.toEpochMilli())
                    statement.setString(7, status.name)
                    statement.executeUpdate()
                }
                Unit
            }
        } catch (exception: SQLException) {
            throw KitClaimDataException("unable to record kit claim for ${player.id}", exception)
        }
    }

    private fun countClaims(connection: Connection, playerId: UUID): Int =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                COUNT(*)
            FROM
                ${UtilitySqlSchema.KIT_CLAIMS}
            WHERE
                namespace = ? AND player_uuid = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.executeQuery().use { rs -> rs.next(); rs.getInt(1) }
        }

    private fun hasClaim(
        connection: Connection,
        playerId: UUID,
        name: KitName
    ): Boolean =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                1
            FROM
                ${UtilitySqlSchema.KIT_CLAIMS}
            WHERE
                namespace = ? AND player_uuid = ? AND kit_name = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, playerId.toString())
            statement.setString(3, name.value)
            statement.executeQuery().use { rs -> rs.next() }
        }

    private fun parseStatus(raw: String): KitClaimStatus =
        KitClaimStatus.entries.firstOrNull { it.name == raw }
            ?: throw KitClaimDataException("kit claims contain an unknown status '$raw'")

    private fun parseUuid(raw: String, label: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            throw KitClaimDataException("kit claims contain an invalid $label")
        }

}
