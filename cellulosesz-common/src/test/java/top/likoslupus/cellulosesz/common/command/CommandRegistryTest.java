package top.likoslupus.cellulosesz.common.command;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
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
    void rollbackHandleRemovesContributionAndFreezeRejectsNewOnes() {
        var registry = new CommandRegistry();
        var handle = registry.register("main", new TestContributor("home"));
        handle.close();

        assertEquals(0, registry.size());
        registry.freezeAndSnapshot();

        assertThrows(
                IllegalStateException.class,
                () -> registry.register("main", new TestContributor("warp"))
        );
        assertTrue(handle.closed());
    }

    @Test
    void repeatedSnapshotsDoNotDuplicateContributors() {
        var registry = new CommandRegistry();
        registry.register("main", new TestContributor("home"));

        assertEquals(registry.freezeAndSnapshot(), registry.freezeAndSnapshot());
        assertEquals(1, registry.snapshot().size());
    }

    @NullMarked
    private record TestContributor(String moduleId) implements CommandContributor {

        @Override
        public void register(CommandRegistrationContext context) {
        }

    }

}
