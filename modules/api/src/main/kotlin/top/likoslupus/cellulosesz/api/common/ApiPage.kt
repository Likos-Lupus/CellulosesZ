package top.likoslupus.cellulosesz.api.common

/**
 * A bounded page of items. [items] is an immutable copy owned by the caller. [nextCursor] is opaque
 * and only meaningful to the producer; `null` means there are no further pages. This keeps APIs from
 * dumping an unbounded export (whole-server identities, every warp, every mail) into a caller.
 */
public class ApiPage<T>(
    public val items: List<T>,
    public val nextCursor: String?,
)
