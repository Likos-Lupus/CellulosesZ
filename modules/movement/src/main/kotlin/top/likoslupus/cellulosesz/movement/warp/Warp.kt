package top.likoslupus.cellulosesz.movement.warp

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

@JvmInline
internal value class WarpName private constructor(val value: String) {

    companion object {

        private val PATTERN = Regex("[a-z0-9_-]{1,32}")

        fun parse(raw: String): WarpName? =
            if (PATTERN.matches(raw)) {
                WarpName(raw)
            } else {
                null
            }

    }

}

internal data class Warp(
    val name: WarpName,
    val position: StoredPosition,
)
