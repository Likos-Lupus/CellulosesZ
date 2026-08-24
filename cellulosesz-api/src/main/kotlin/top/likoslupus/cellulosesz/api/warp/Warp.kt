package top.likoslupus.cellulosesz.api.warp

import top.likoslupus.cellulosesz.api.teleport.CellLocation
import top.likoslupus.cellulosesz.api.validation.requireNonBlank
import top.likoslupus.cellulosesz.api.validation.requireNonNegative
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/** Immutable warp value. */
@JvmRecord
public data class Warp(
    public val name: String,
    public val displayName: String,
    public val cost: BigDecimal,
    public val location: CellLocation,
    public val createdBy: UUID?,
    public val createdAt: Instant
) {

    public constructor(name: String, location: CellLocation) : this(
        name,
        name,
        BigDecimal.ZERO,
        location,
        null,
        Instant.now()
    )

    init {
        name.requireNonBlank { "name" }
        displayName.requireNonBlank { "displayName" }
        cost.requireNonNegative { "cost" }
    }

}
