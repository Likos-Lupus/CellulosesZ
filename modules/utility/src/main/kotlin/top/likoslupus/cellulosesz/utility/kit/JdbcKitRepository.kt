package top.likoslupus.cellulosesz.utility.kit

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.minecraft.world.item.ItemStack
import top.likoslupus.cellulosesz.foundation.database.DatabaseRuntime
import top.likoslupus.cellulosesz.utility.UtilitySqlSchema
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
import java.time.Duration
import java.util.*

/** The kit catalog in `cz_kits` + `cz_kit_items`. Create/replace/remove stay distinct. */
internal class JdbcKitRepository(
    private val database: DatabaseRuntime,
    private val namespace: String,
    private val codec: KitItemCodec,
) : KitRepository {

    override suspend fun loadAll(): Map<KitName, KitDefinition> =
        withContext(Dispatchers.IO) {
            try {
                database.read { connection ->
                    val kits = LinkedHashMap<KitName, KitDefinition>()
                    connection.prepareStatement(
                        /* language=SQL */ """
                        SELECT
                            kit_id,
                            name,
                            reuse_mode,
                            cooldown_seconds
                        FROM
                            ${UtilitySqlSchema.KITS}
                        WHERE
                            namespace = ?
                        ORDER BY
                            name;
                        """.trimIndent()
                    ).use { statement ->
                        statement.setString(1, namespace)
                        statement.executeQuery().use { rs ->
                            while (rs.next()) {
                                val kitId = parseUuid(rs.getString(1), "kit id")
                                val name = KitName.parse(rs.getString(2))
                                    ?: throw KitDataException("kit catalog contains an invalid kit name")
                                val mode = parseMode(rs.getString(3))
                                val cooldown = rs.getObject(4)?.let { rs.getLong(4) }
                                kits[name] = KitDefinition(
                                    id = KitId(kitId),
                                    name = name,
                                    reuse = reusePolicy(name, mode, cooldown),
                                    items = loadItems(connection, kitId, name),
                                )
                            }
                        }
                    }
                    if (kits.size > MAX_STORED_KITS) {
                        throw KitDataException("kit catalog exceeds the stored kit cap")
                    }
                    kits
                }
            } catch (exception: SQLException) {
                throw KitDataException("unable to read kit catalog", exception)
            }
        }

    override suspend fun create(definition: KitDefinition): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    if (exists(connection, definition.name)) {
                        return@transaction false
                    }
                    insertKit(connection, definition)
                    true
                }
            } catch (exception: SQLException) {
                throw KitDataException("unable to create kit '${definition.name.value}'", exception)
            }
        }

    override suspend fun replace(definition: KitDefinition): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    val existing = existingKitId(connection, definition.name)
                        ?: return@transaction false
                    deleteKit(connection, existing)
                    insertKit(connection, definition)
                    true
                }
            } catch (exception: SQLException) {
                throw KitDataException(
                    "unable to replace kit '${definition.name.value}'",
                    exception
                )
            }
        }

    override suspend fun remove(name: KitName): Boolean =
        withContext(Dispatchers.IO) {
            try {
                database.transaction { connection ->
                    val existing = existingKitId(connection, name) ?: return@transaction false
                    deleteKit(connection, existing)
                    true
                }
            } catch (exception: SQLException) {
                throw KitDataException("unable to remove kit '${name.value}'", exception)
            }
        }

    private fun exists(connection: Connection, name: KitName): Boolean =
        existingKitId(connection, name) != null

    private fun existingKitId(connection: Connection, name: KitName): UUID? =
        connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                kit_id
            FROM
                ${UtilitySqlSchema.KITS}
            WHERE
                namespace = ? AND name = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, name.value)
            statement.executeQuery().use { rs ->
                when {
                    rs.next() -> parseUuid(rs.getString(1), "kit id")
                    else -> null
                }
            }
        }

    private fun deleteKit(connection: Connection, kitId: UUID) {
        connection.prepareStatement(
            /* language=SQL */ """
            DELETE FROM
                ${UtilitySqlSchema.KIT_ITEMS}
            WHERE
                namespace = ? AND kit_id = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, kitId.toString())
            statement.executeUpdate()
        }
        connection.prepareStatement(
            /* language=SQL */ """
            DELETE FROM
                ${UtilitySqlSchema.KITS}
            WHERE
                namespace = ? AND kit_id = ?;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, kitId.toString())
            statement.executeUpdate()
        }
    }

    private fun insertKit(connection: Connection, definition: KitDefinition) {
        val mode: KitReuseMode
        val cooldown: Long?
        when (val policy = definition.reuse) {
            KitReusePolicy.Always -> {
                mode = KitReuseMode.ALWAYS
                cooldown = null
            }

            KitReusePolicy.Once -> {
                mode = KitReuseMode.ONCE
                cooldown = null
            }

            is KitReusePolicy.Cooldown -> {
                mode = KitReuseMode.COOLDOWN
                cooldown = policy.duration.seconds
            }
        }
        connection.prepareStatement(
            /* language=SQL */ """
            INSERT INTO ${UtilitySqlSchema.KITS} (
                namespace,
                kit_id,
                name,
                reuse_mode,
                cooldown_seconds
            )
            VALUES (
                ?, ?, ?, ?, ?
            );
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, definition.id.value.toString())
            statement.setString(3, definition.name.value)
            statement.setString(4, mode.name)
            cooldown?.let { statement.setLong(5, it) }
                ?: statement.setNull(5, Types.BIGINT)
            statement.executeUpdate()
        }
        if (definition.items.isNotEmpty()) {
            connection.prepareStatement(
                /* language=SQL */ """
                INSERT INTO ${UtilitySqlSchema.KIT_ITEMS} (
                    namespace,
                    kit_id,
                    ordinal,
                    payload
                )
                VALUES (
                    ?, ?, ?, ?
                );
                """.trimIndent()
            ).use { statement ->
                definition.items.forEachIndexed { index, stack ->
                    statement.setString(1, namespace)
                    statement.setString(2, definition.id.value.toString())
                    statement.setInt(3, index)
                    statement.setString(4, codec.encode(stack))
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }

    private fun loadItems(
        connection: Connection,
        kitId: UUID,
        name: KitName
    ): List<ItemStack> {
        val payloads = connection.prepareStatement(
            /* language=SQL */ """
            SELECT
                payload
            FROM
                ${UtilitySqlSchema.KIT_ITEMS}
            WHERE
                namespace = ? AND kit_id = ?
            ORDER BY
                ordinal;
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, namespace)
            statement.setString(2, kitId.toString())
            statement.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(rs.getString(1))
                    }
                }
            }
        }
        if (payloads.size > MAX_STORED_ITEMS_PER_KIT) {
            throw KitDataException("kit '${name.value}' exceeds the stored item cap")
        }
        return payloads.mapIndexed { index, payload ->
            if (payload.length > MAX_ITEM_PAYLOAD_CHARS) {
                throw KitDataException("invalid item $index in kit '${name.value}': payload too large")
            }
            val stack = try {
                codec.decode(payload)
            } catch (exception: Exception) {
                throw KitDataException("invalid item $index in kit '${name.value}'", exception)
            }
            if (stack.isEmpty || stack.count <= 0) {
                throw KitDataException("invalid item $index in kit '${name.value}': empty stack")
            }
            stack
        }
    }

    private fun parseMode(raw: String): KitReuseMode =
        KitReuseMode.entries.firstOrNull { it.name == raw }
            ?: throw KitDataException("kit catalog contains an unknown reuse mode '$raw'")

    private fun reusePolicy(
        name: KitName,
        mode: KitReuseMode,
        cooldown: Long?
    ): KitReusePolicy =
        when (mode) {
            KitReuseMode.ALWAYS -> {
                if (cooldown != null) {
                    throw KitDataException("kit '${name.value}' has an unexpected cooldown")
                }
                KitReusePolicy.Always
            }

            KitReuseMode.ONCE -> {
                if (cooldown != null) {
                    throw KitDataException("kit '${name.value}' has an unexpected cooldown")
                }
                KitReusePolicy.Once
            }

            KitReuseMode.COOLDOWN -> {
                if (cooldown == null || cooldown <= 0L) {
                    throw KitDataException("kit '${name.value}' is a cooldown kit without a positive cooldown")
                }
                KitReusePolicy.Cooldown(Duration.ofSeconds(cooldown))
            }
        }

    private fun parseUuid(raw: String, label: String): UUID =
        try {
            UUID.fromString(raw)
        } catch (_: IllegalArgumentException) {
            throw KitDataException("kit catalog has an invalid $label")
        }

}
