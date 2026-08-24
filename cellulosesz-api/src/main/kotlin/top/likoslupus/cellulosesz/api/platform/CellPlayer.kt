package top.likoslupus.cellulosesz.api.platform

import top.likoslupus.cellulosesz.api.validation.requireNonBlank
import java.util.*

/** Stable platform-neutral player identity. */
@JvmRecord
public data class CellPlayer(
    public val uuid: UUID,
    public val name: String
) {

    init {
        name.requireNonBlank { "name" }
    }

}
