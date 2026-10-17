package top.likoslupus.cellulosesz.economy.ledger

/**
 * Raised when stored ledger data is corrupt (negative player balance, unknown account kind, an
 * unbalanced transaction, a missing transaction header) or when storage fails unexpectedly. Such a
 * state is never silently repaired or interpreted as a zero balance.
 */
internal class EconomyDataException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
