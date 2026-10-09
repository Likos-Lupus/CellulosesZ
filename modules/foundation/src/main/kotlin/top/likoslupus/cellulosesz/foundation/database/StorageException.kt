package top.likoslupus.cellulosesz.foundation.database

/** Unexpected storage failure (connectivity, schema, SQL). Expected business conflicts are values. */
public class StorageException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
