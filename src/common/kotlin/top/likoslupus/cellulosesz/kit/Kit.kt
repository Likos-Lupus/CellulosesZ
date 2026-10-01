package top.likoslupus.cellulosesz.kit

import top.likoslupus.cellulosesz.naming.LocationNames

@JvmInline
internal value class KitName private constructor(val value: String) {

    companion object {

        fun parse(raw: String): KitName? =
            if (LocationNames.isValid(raw)) {
                KitName(raw)
            } else {
                null
            }

    }

}
