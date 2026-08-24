package top.likoslupus.cellulosesz.api.permission

import top.likoslupus.cellulosesz.api.platform.CellPlayer

/**
 * Permission queries for a stable player identity. Backend failures are propagated, not represented
 * as absence.
 */
public interface PermissionService {

    public fun has(
        player: CellPlayer,
        permission: String
    ): Boolean

    public fun intOption(
        player: CellPlayer,
        key: String,
        fallback: Int
    ): Int

    public fun boolOption(
        player: CellPlayer,
        key: String,
        fallback: Boolean
    ): Boolean

    public fun stringOption(
        player: CellPlayer,
        key: String
    ): String?

}
