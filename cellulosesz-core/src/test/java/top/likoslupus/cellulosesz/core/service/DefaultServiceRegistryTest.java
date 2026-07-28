package top.likoslupus.cellulosesz.core.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultServiceRegistryTest {

    @Test
    void duplicateRegistrationFailsAndCloseIsIdentitySafe() {
        var registry = new DefaultServiceRegistry();
        var first = new Object();
        var registration = registry.register(Object.class, first, "module-a");
        assertThrows(IllegalStateException.class,
                () -> registry.register(Object.class, new Object(), "module-b"));
        assertSame(first, registry.require(Object.class));
        registration.close();
        assertFalse(registry.contains(Object.class));
        registration.close();
    }
}
