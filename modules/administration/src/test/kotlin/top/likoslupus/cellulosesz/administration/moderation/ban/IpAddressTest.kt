package top.likoslupus.cellulosesz.administration.moderation.ban

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IpAddressTest {

    @Test
    fun `accepts and preserves IPv4`() {
        assertEquals(
            "1.2.3.4",
            IpAddress.parse("1.2.3.4")?.value
        )
        assertEquals(
            "1.2.3.4",
            IpAddress.parse("  1.2.3.4  ")?.value
        )
    }

    @Test
    fun `canonicalizes IPv6`() {
        assertEquals(
            "2001:db8::1",
            IpAddress.parse("2001:0db8:0000:0000:0000:0000:0000:0001")?.value
        )
        assertEquals(
            "::1",
            IpAddress.parse("0:0:0:0:0:0:0:1")?.value
        )
    }

    @Test
    fun `rejects hostnames and invalid input`() {
        assertNull(IpAddress.parse("example.com"))
        assertNull(IpAddress.parse("localhost"))
        assertNull(IpAddress.parse(""))
        assertNull(IpAddress.parse("   "))
        assertNull(IpAddress.parse("999.999.999.999"))
        assertNull(IpAddress.parse("1.2.3"))
        assertNull(IpAddress.parse(":"))
        assertNull(IpAddress.parse("1:2:3"))
    }

}
