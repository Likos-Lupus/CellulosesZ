package top.likoslupus.cellulosesz.core.preference

import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import java.sql.SQLException
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public class JdbcPlayerPreferenceRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
) : PlayerPreferenceRepository {

    public override fun loadAllBlocking(): Map<UUID, Map<PlayerPreference, Boolean>> =
        try {
            database.read { connection ->
                connection.prepareStatement(
                    /* language=SQL */ """
                    SELECT
                        player_uuid,
                        preference_key,
                        preference_value
                    FROM
                        ${PreferenceSqlSchema.PREFERENCES}
                    WHERE
                        namespace = ?;
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, namespace)
                    statement.executeQuery().use { rs ->
                        val hydrated = HashMap<UUID, MutableMap<PlayerPreference, Boolean>>()
                        while (rs.next()) {
                            val playerId = parseUuid(rs.getString(1))
                            val key = PlayerPreference.fromStorageKey(rs.getString(2))
                                ?: throw PlayerPreferenceDataException(
                                    "unknown preference key '${rs.getString(2)}'"
                                )
                            hydrated.computeIfAbsent(
                                playerId
                            ) { HashMap() }[key] = rs.getBoolean(3)
                        }
                        hydrated.mapValues { (_, values) -> values.toMap() }
                    }
                }
            }
        } catch (exception: SQLException) {
            throw PlayerPreferenceDataException("unable to load player preferences", exception)
        }

    public override suspend fun save(
        playerId: UUID,
        preference: PlayerPreference,
        enabled: Boolean,
    ) {
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    connection.prepareStatement(
                        /* language=SQL */ """
                        DELETE FROM
                            ${PreferenceSqlSchema.PREFERENCES}
                        WHERE
                            namespace = ? AND player_uuid = ? AND preference_key = ?;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.setString(3, preference.storageKey)
                        statement.executeUpdate()
                    }
                    connection.prepareStatement(
                        /* language=SQL */ """
                        INSERT INTO ${PreferenceSqlSchema.PREFERENCES} (
                            namespace,
                            player_uuid,
                            preference_key,
                            preference_value
                        )
                        VALUES (
                            ?, ?, ?, ?
                        );
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.setString(2, playerId.toString())
                        statement.setString(3, preference.storageKey)
                        statement.setBoolean(4, enabled)
                        statement.executeUpdate()
                    }
                    Unit
                }
            } catch (exception: SQLException) {
                throw PlayerPreferenceDataException(
                    "unable to write preference ${preference.storageKey} for $playerId",
                    exception
                )
            }
        }
    }

    private fun parseUuid(raw: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            throw PlayerPreferenceDataException("invalid preference player UUID '$raw'")
        }

}
