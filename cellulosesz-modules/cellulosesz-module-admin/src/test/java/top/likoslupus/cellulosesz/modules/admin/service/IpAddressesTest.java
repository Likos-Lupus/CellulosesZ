package top.likoslupus.cellulosesz.modules.admin.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class IpAddressesTest {

    @Test
    void normalizesIpv4AndMappedIpv6() {
        assertEquals("192.0.2.1", IpAddresses.normalize("192.0.2.1").orElseThrow());
        assertEquals("192.0.2.1", IpAddresses.normalize("::ffff:192.0.2.1").orElseThrow());
    }

    @Test
    void rejectsHostnamesAndCidrs() {
        assertTrue(IpAddresses.normalize("example.com").isEmpty());
        assertTrue(IpAddresses.normalize("192.0.2.0/24").isEmpty());
    }

}
