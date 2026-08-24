package top.likoslupus.cellulosesz.api.economy

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.util.*

class EconomyContractsTest {

    @Test
    fun balanceEntry_properties() {
        val uuid = UUID.randomUUID()
        val entry = BalanceEntry(uuid, BigDecimal("100.50"))
        assertEquals(uuid, entry.uuid)
        assertEquals(BigDecimal("100.50"), entry.balance)
    }

    @Test
    fun balanceFilter_validation() {
        val all = BalanceFilter.all()
        assertNull(all.minimum)
        assertNull(all.maximum)

        val valid = BalanceFilter(BigDecimal("10"), BigDecimal("100"))
        assertEquals(BigDecimal("10"), valid.minimum)
        assertEquals(BigDecimal("100"), valid.maximum)

        assertThrows<IllegalArgumentException> {
            BalanceFilter(BigDecimal("100"), BigDecimal("10"))
        }
    }

    @Test
    fun transactionResult_factories() {
        val success = TransactionResult.success(
            "economy.deposit.success",
            BigDecimal("10.00"),
            BigDecimal("110.00")
        )
        assertTrue(success.success)
        assertEquals(BigDecimal("10.00"), success.amount)
        assertEquals(BigDecimal("110.00"), success.balance)
    }

}
