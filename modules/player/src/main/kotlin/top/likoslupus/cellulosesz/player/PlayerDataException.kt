package top.likoslupus.cellulosesz.player

/** Raised when player machine data is missing, corrupt, or unsafe to load. Never silently repaired. */
internal class PlayerDataException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
