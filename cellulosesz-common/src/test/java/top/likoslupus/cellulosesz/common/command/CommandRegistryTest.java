package top.likoslupus.cellulosesz.common.command;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.NullMarked;

import static org.junit.jupiter.api.Assertions.*;

final class CommandRegistryTest {

    @Test
    void registersInStableIdentityOrderAndReturnsImmutableSnapshot() {
        var registry = new CommandRegistry();
        var warp = new TestContributor("warp");
        var home = new TestContributor("home");

        registry.register("main", warp);
        registry.register("main", home);

        var snapshot = new ArrayList<>(registry.snapshot());
        assertEquals(List.of(home, warp), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(home));
    }

    @Test
    void duplicateIdentityAndInstanceFail() {
        var registry = new CommandRegistry();
        var contributor = new TestContributor("home");

        registry.register("main", contributor);
        assertThrows(
                IllegalStateException.class,
                () -> registry.register("main", new TestContributor("home"))
        );
        assertThrows(
                IllegalStateException.class,
                () -> registry.register("other", contributor)
        );
    }

    @Test
    void closedContributorCanBeRegisteredAgainForDynamicModuleReload() {
        var registry = new CommandRegistry();
        var first = new TestContributor("home");
        var handle = registry.register("main", first);

        handle.close();
        handle.close();
        assertTrue(handle.closed());
        assertEquals(List.of(), registry.snapshot());

        var replacement = new TestContributor("home");
        registry.register("main", replacement);
        assertEquals(List.of(replacement), registry.snapshot());
    }

    @Test
    void repeatedSnapshotsDoNotDuplicateContributors() {
        var registry = new CommandRegistry();
        registry.register("main", new TestContributor("home"));

        assertEquals(registry.snapshot(), registry.snapshot());
        assertEquals(1, registry.snapshot().size());
    }

    @NullMarked
    private record TestContributor(String moduleId) implements CommandContributor {

        @Override
        public void register(CommandRegistrationContext context) {
        }

    }

}
