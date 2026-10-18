package top.likoslupus.cellulosesz.foundation

import java.util.*

/**
 * Parses a UUID read from storage and delegates the invalid case to [onInvalid], which must throw.
 * Keeping the failure in the caller is deliberate: every repository owns a typed data exception, so
 * the shared helper removes the repeated `try`/`catch` without flattening those error types.
 */
public inline fun parseUuidOrThrow(
    raw: String,
    onInvalid: (String) -> Nothing,
): UUID =
    parseUuidOrThrow(raw) { invalid, _ -> onInvalid(invalid) }

/**
 * The cause-preserving overload. Use it when the repository wants to keep the
 * [IllegalArgumentException] as the typed exception's cause (for example a corrupt row diagnostic).
 */
public inline fun parseUuidOrThrow(
    raw: String,
    onInvalid: (String, IllegalArgumentException) -> Nothing,
): UUID =
    try {
        UUID.fromString(raw)
    } catch (exception: IllegalArgumentException) {
        onInvalid(raw, exception)
    }
