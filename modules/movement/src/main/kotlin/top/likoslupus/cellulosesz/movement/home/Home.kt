package top.likoslupus.cellulosesz.movement.home

import top.likoslupus.cellulosesz.movement.teleport.StoredPosition

@JvmInline
internal value class HomeName private constructor(val value: String) {

    companion object {

        private val PATTERN = Regex("[a-z0-9_-]{1,32}")

        fun parse(raw: String): HomeName? =
            if (PATTERN.matches(raw)) {
                HomeName(raw)
            } else {
                null
            }

    }

}

internal data class Home(
    val name: HomeName,
    val position: StoredPosition,
)
