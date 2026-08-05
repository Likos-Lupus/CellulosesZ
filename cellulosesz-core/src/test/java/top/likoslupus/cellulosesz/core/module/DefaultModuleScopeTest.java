package top.likoslupus.cellulosesz.core.module;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.service.Registration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultModuleScopeTest {

    @Test
    void close_withAsyncAndFailures_usesReverseOrderAndContinues() {
        var order = new ArrayList<String>();
        var scope = new DefaultModuleScope("test");
        var asyncGate = new CompletableFuture<Void>();

        scope.own(new TestRegistration("test", "first", order, false));
        scope.own(new TestCloseable("async", order, asyncGate));
        scope.own(new TestRegistration("test", "last", order, true));

        var close = scope.closeAsync();
        assertSame(close, scope.closeAsync());
        assertEquals(List.of("last", "async-stop", "async-drain"), order);
        assertFalse(close.isDone());

        asyncGate.complete(null);
        var failure = assertThrows(RuntimeException.class, close::join);
        assertEquals(List.of("last", "async-stop", "async-drain", "first"), order);
        assertEquals(1, failure.getCause().getSuppressed().length);
    }

    @Test
    void register_whenOwnerMismatch_closesWithoutLeak() {
        var order = new ArrayList<String>();
        var scope = new DefaultModuleScope("expected");
        var registration = new TestRegistration("other", "closed", order, false);

        assertThrows(IllegalArgumentException.class, () -> scope.own(registration));
        assertTrue(registration.closed());
        assertEquals(List.of("closed"), order);
        scope.closeAsync().join();
        assertEquals(List.of("closed"), order);
    }

    private static final class TestRegistration implements Registration {

        private final String owner;
        private final String label;
        private final List<String> order;
        private final boolean fail;
        private boolean closed;

        private TestRegistration(
                String owner,
                String label,
                List<String> order,
                boolean fail
        ) {
            this.owner = owner;
            this.label = label;
            this.order = order;
            this.fail = fail;
        }

        @Override
        public String owner() {
            return owner;
        }

        @Override
        public boolean closed() {
            return closed;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            closed = true;
            order.add(label);
            if (fail) {
                throw new IllegalStateException(label);
            }
        }

    }

    private record TestCloseable(
            String label,
            List<String> order,
            CompletableFuture<Void> gate
    ) implements AsyncCloseable {

        @Override
        public void stopAccepting() {
            order.add(label + "-stop");
        }

        @Override
        public CompletableFuture<Void> drain() {
            order.add(label + "-drain");
            return gate;
        }

    }

}
