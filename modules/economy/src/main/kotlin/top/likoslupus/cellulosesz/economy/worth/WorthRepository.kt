package top.likoslupus.cellulosesz.economy.worth

/**
 * The single owner of `cz_economy_worth`. Reads return every row for the namespace; the write is an
 * idempotent upsert keyed `(namespace, item_id)` so re-importing a seed never duplicates a price.
 */
internal interface WorthRepository {

    /** Every worth row of the namespace, in no particular order. */
    suspend fun loadAll(): List<WorthRecord>

    /** Inserts or replaces one item's price. */
    suspend fun upsert(record: WorthRecord)

    /** Inserts every record that is not already priced; existing prices are left untouched. */
    suspend fun insertMissing(records: List<WorthRecord>)

    /** Number of rows currently stored for the namespace. */
    suspend fun count(): Int

}
