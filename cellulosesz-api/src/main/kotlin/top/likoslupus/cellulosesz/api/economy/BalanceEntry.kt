package top.likoslupus.cellulosesz.api.economy

import java.math.BigDecimal
import java.util.*

@JvmRecord
public data class BalanceEntry(
    public val uuid: UUID,
    public val balance: BigDecimal
)
