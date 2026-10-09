package top.likoslupus.cellulosesz.foundation.database.schema

import top.likoslupus.cellulosesz.foundation.database.DatabaseType

/**
 * The small set of places where the five supported databases genuinely differ. Everything else
 * (table DDL shape, column list, probe) is generated identically from [SqlTable]/[SqlIndex].
 */
public interface SqlDialect {

    public val type: DatabaseType

    public fun quote(identifier: String): String

    public fun columnType(type: SqlColumnType): String

    public fun booleanValue(value: Boolean): String

    /** Full `CREATE TABLE IF NOT EXISTS` statement. */
    public fun createTableSql(table: SqlTable): String

    /** Full `CREATE [UNIQUE] INDEX` statement. May lack `IF NOT EXISTS` on MySQL/MariaDB. */
    public fun createIndexSql(index: SqlIndex): String

    /** True when duplicate-index errors must be swallowed because the statement lacks IF NOT EXISTS. */
    public val createIndexMayDuplicate: Boolean

    /** Portable single-row upsert keyed by [keyColumns]; only non-key [columns] are updated. */
    public fun upsertSql(
        table: String,
        keyColumns: List<String>,
        columns: List<String>
    ): String

    /** `SELECT <columns> FROM <table> WHERE 1 = 0` used as a schema compatibility probe. */
    public fun probeSql(table: SqlTable): String

}

public object SqlDialects {

    public fun of(type: DatabaseType): SqlDialect =
        when (type) {
            DatabaseType.SQLITE -> SqliteDialect
            DatabaseType.H2 -> H2Dialect
            DatabaseType.MYSQL -> MySqlDialect
            DatabaseType.MARIADB -> MariaDbDialect
            DatabaseType.POSTGRESQL -> PostgreSqlDialect
        }

}

private fun quotesTablePrefix(): String =
    "cz_"

private abstract class BaseSqlDialect(
    override val type: DatabaseType,
) : SqlDialect {

    // All CellulosesZ identifiers are safe lowercase [a-z0-9_], so they are emitted unquoted. This
    // keeps DDL and DML folding identically on H2 (upper) and PostgreSQL (lower).
    override fun quote(identifier: String): String =
        identifier

    override fun columnType(type: SqlColumnType): String =
        when (type) {
            SqlColumnType.UUID -> "VARCHAR(36)"
            SqlColumnType.NAME -> "VARCHAR(32)"
            SqlColumnType.RESOURCE -> "VARCHAR(255)"
            SqlColumnType.HASH -> "VARCHAR(128)"
            SqlColumnType.TEXT -> "TEXT"
            SqlColumnType.BIGINT -> "BIGINT"
            SqlColumnType.INTEGER -> "INTEGER"
            SqlColumnType.DOUBLE -> "DOUBLE"
            SqlColumnType.REAL -> "REAL"
            SqlColumnType.BOOLEAN -> "BOOLEAN"
        }

    override fun booleanValue(value: Boolean): String =
        if (value) "1" else "0"

    /* language=SQL */
    override fun createTableSql(table: SqlTable): String =
        buildString {
            append("CREATE TABLE IF NOT EXISTS ").append(quote(table.name)).append(" (")

            table.columns.forEachIndexed { index, column ->
                if (index > 0) append(", ")
                append(quote(column.name)).append(' ').append(columnType(column.type))
                if (!column.nullable) append(" NOT NULL")
            }

            if (table.primaryKey.isNotEmpty()) {
                append(", PRIMARY KEY (")
                append(table.primaryKey.joinToString(", ") { quote(it) })
                append(')')
            }

            append(')')
            append(tableSuffix())
        }

    /* language=SQL */
    override fun createIndexSql(index: SqlIndex): String =
        buildString {
            append("CREATE ")
            if (index.unique) append("UNIQUE ")
            append("INDEX IF NOT EXISTS ").append(quote(index.name))
            append(" ON ").append(quote(index.table))
            append(" (").append(index.columns.joinToString(", ") { quote(it) }).append(')')
        }

    override val createIndexMayDuplicate: Boolean
        get() = false

    override fun probeSql(table: SqlTable): String =
        /* language=SQL */ """
        SELECT
            ${table.columns.joinToString(", ") { quote(it.name) }}
        FROM
            ${quote(table.name)}
        WHERE
            1 = 0;
        """.trimIndent()

    protected open fun tableSuffix(): String =
        ""

}

private object SqliteDialect : BaseSqlDialect(DatabaseType.SQLITE) {

    override fun upsertSql(
        table: String,
        keyColumns: List<String>,
        columns: List<String>
    ): String =
        onConflictUpsert(table, keyColumns, columns)

}

private object H2Dialect : BaseSqlDialect(DatabaseType.H2) {

    override fun upsertSql(
        table: String,
        keyColumns: List<String>,
        columns: List<String>
    ): String =
        /* language=SQL */ """
        MERGE INTO ${quote(table)} (
            ${columns.joinToString(", ") { quote(it) }}
        )
        KEY (
            ${keyColumns.joinToString(", ") { quote(it) }}
        )
        VALUES (
            ${columns.joinToString(", ") { "?" }}
        );
        """.trimIndent()

}

private object PostgreSqlDialect : BaseSqlDialect(DatabaseType.POSTGRESQL) {

    override fun columnType(type: SqlColumnType): String =
        when (type) {
            SqlColumnType.DOUBLE -> "DOUBLE PRECISION"
            else -> super.columnType(type)
        }

    override fun upsertSql(
        table: String,
        keyColumns: List<String>,
        columns: List<String>
    ): String =
        onConflictUpsert(table, keyColumns, columns)

}

private object MySqlDialect : MySqlFamilyDialect(DatabaseType.MYSQL)

private object MariaDbDialect : MySqlFamilyDialect(DatabaseType.MARIADB)

private abstract class MySqlFamilyDialect(type: DatabaseType) : BaseSqlDialect(type) {

    override val createIndexMayDuplicate: Boolean get() = true

    /* language=SQL */
    override fun createIndexSql(index: SqlIndex): String =
        buildString {
            append("CREATE ")
            if (index.unique) append("UNIQUE ")
            append("INDEX ").append(quote(index.name))
            append(" ON ").append(quote(index.table))
            append(" (").append(index.columns.joinToString(", ") { quote(it) }).append(')')
        }

    override fun upsertSql(
        table: String,
        keyColumns: List<String>,
        columns: List<String>
    ): String =
        /* language=SQL */ """
    INSERT INTO ${quote(table)} (
        ${columns.joinToString(", ") { quote(it) }}
    )
    VALUES (
        ${columns.joinToString(", ") { "?" }}
    )
    ON DUPLICATE KEY UPDATE
    ${
        columns.filterNot { it in keyColumns }
                .joinToString(", ") {
                    "${quote(it)} = VALUES(${quote(it)})"
                }
    }
    """.trimIndent()

    override fun tableSuffix(): String =
        /* language=SQL */
        " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4"

}

private fun onConflictUpsert(
    table: String,
    keyColumns: List<String>,
    columns: List<String>
): String =
    /* language=SQL */ """
    INSERT INTO $table (
        ${columns.joinToString(", ")}
    )
    VALUES (
        ${columns.joinToString(", ") { "?" }}
    )
    ON CONFLICT (
        ${keyColumns.joinToString(", ")}
    )
    DO UPDATE SET
        ${columns.filterNot { it in keyColumns }.joinToString(", ") { "$it = excluded.$it" }}
    """.trimIndent()
