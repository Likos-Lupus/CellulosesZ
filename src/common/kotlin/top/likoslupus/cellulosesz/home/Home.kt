package top.likoslupus.cellulosesz.home

import top.likoslupus.cellulosesz.naming.LocationNames
import top.likoslupus.cellulosesz.teleport.StoredPosition

@JvmInline
internal value class HomeName private constructor(val value: String) {

    companion object {

        fun parse(raw: String): HomeName? =
            if (LocationNames.isValid(raw)) {
                HomeName(raw)
            } else {
                null
            }

    }

}

internal data class Home(
    val name: HomeName,
    val position: StoredPosition
)
