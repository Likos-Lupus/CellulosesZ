package top.likoslupus.cellulosesz.warp

import top.likoslupus.cellulosesz.naming.LocationNames
import top.likoslupus.cellulosesz.teleport.StoredPosition

@JvmInline
internal value class WarpName private constructor(val value: String) {

    companion object {

        fun parse(raw: String): WarpName? =
            if (LocationNames.isValid(raw)) {
                WarpName(raw)
            } else {
                null
            }

    }

}

internal data class Warp(
    val name: WarpName,
    val position: StoredPosition
)
