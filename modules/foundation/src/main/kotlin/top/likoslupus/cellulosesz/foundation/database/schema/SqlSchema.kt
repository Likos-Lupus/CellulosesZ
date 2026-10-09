package top.likoslupus.cellulosesz.foundation.database.schema

/** Portable column types; the dialect maps these onto vendor SQL types. */
public enum class SqlColumnType {

    UUID,
    NAME,
    RESOURCE,
    HASH,
    TEXT,
    BIGINT,
    INTEGER,
    DOUBLE,
    REAL,
    BOOLEAN,

}

public data class SqlColumn(
    public val name: String,
    public val type: SqlColumnType,
    public val nullable: Boolean = false,
)

/** A fixed `cz_*` table. The `namespace` column is part of the logical key, never a table suffix. */
public data class SqlTable(
    public val name: String,
    public val columns: List<SqlColumn>,
    public val primaryKey: List<String>,
)

public data class SqlIndex(
    public val name: String,
    public val table: String,
    public val columns: List<String>,
    public val unique: Boolean = false,
)

/** A bounded context contributes its own tables and indexes; application only aggregates them. */
public interface SqlSchemaContributor {

    public val id: String
    public fun tables(): List<SqlTable>
    public fun indexes(): List<SqlIndex>

}
