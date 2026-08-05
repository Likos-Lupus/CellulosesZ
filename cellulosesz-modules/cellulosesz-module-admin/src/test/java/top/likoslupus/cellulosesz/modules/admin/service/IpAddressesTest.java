package top.likoslupus.cellulosesz.modules.admin.service;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IpAddressesTest {

    @Test
    void canonicalize_withMappedIpv6AndBrackets_normalizes() throws Exception {
        assertEquals(
                "192.0.2.1",
                IpAddresses.canonical(IpAddresses.parseLiteral("192.0.2.1").orElseThrow())
        );
        assertEquals(
                "192.0.2.1",
                IpAddresses.canonical(IpAddresses.parseLiteral("::ffff:192.0.2.1").orElseThrow())
        );
        assertEquals(
                InetAddress.getByName("2001:db8::1"),
                IpAddresses.parseLiteral("[2001:db8::1]").orElseThrow()
        );
    }

    @Test
    void parse_withHostnameCidrOrZone_rejectsWithoutDns() {
        assertTrue(IpAddresses.parseLiteral("example.com").isEmpty());
        assertTrue(IpAddresses.parseLiteral("192.0.2.0/24").isEmpty());
        assertTrue(IpAddresses.parseLiteral("fe80::1%eth0").isEmpty());
        assertTrue(IpAddresses.parseLiteral("999.1.1.1").isEmpty());
    }

}
