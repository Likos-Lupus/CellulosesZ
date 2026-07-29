package top.likoslupus.cellulosesz.common.command;

import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.validation.Checks;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

/**
 * Thread-safe registry for module-owned Minecraft command contributions.
 */
public final class CommandRegistry {

    private final Map<Identity, Entry> entries = new LinkedHashMap<>();
    private long sequence;
    private boolean frozen;

    public synchronized Registration register(
            String registrationId,
            CommandContributor contributor
    ) {
        requireNonNull(contributor, "contributor");
        var identity = new Identity(contributor.moduleId(), registrationId);

        if (frozen) {
            throw new IllegalStateException("Command registry is frozen; cannot register " + identity);
        }
        if (entries.containsKey(identity)) {
            throw new IllegalStateException("Duplicate command registration " + identity);
        }
        if (entries.values().stream()
                .anyMatch(entry -> entry.contributor() == contributor)
        ) {
            throw new IllegalStateException("Command contributor instance is already registered: " + identity);
        }

        var entry = new Entry(identity, contributor, sequence++);
        entries.put(identity, entry);
        return new Handle(this, identity);
    }

    public synchronized List<CommandContributor> freezeAndSnapshot() {
        frozen = true;
        return snapshot();
    }

    public synchronized List<CommandContributor> snapshot() {
        return entries.values().stream()
                .sorted(Comparator.comparing((Entry entry) -> entry.identity().moduleId())
                        .thenComparing(entry -> entry.identity().registrationId())
                        .thenComparingLong(Entry::sequence)
                )
                .map(Entry::contributor)
                .toList();
    }

    public synchronized boolean frozen() {
        return frozen;
    }

    public synchronized int size() {
        return entries.size();
    }

    private synchronized void remove(Identity identity) {
        entries.remove(identity);
    }

    public record Identity(
            String moduleId,
            String registrationId
    ) {

        public Identity {
            moduleId = Checks.requireNonBlank(moduleId, "moduleId").trim();
            registrationId = Checks.requireNonBlank(registrationId, "registrationId").trim();
        }

        @Override
        public String toString() {
            return moduleId + ":" + registrationId;
        }

    }

    private record Entry(
            Identity identity,
            CommandContributor contributor,
            long sequence
    ) {

    }

    private static final class Handle implements Registration {

        private final CommandRegistry registry;
        private final Identity identity;
        private final AtomicBoolean closed = new AtomicBoolean();

        private Handle(
                CommandRegistry registry,
                Identity identity
        ) {
            this.registry = registry;
            this.identity = identity;
        }

        @Override
        public String owner() {
            return identity.moduleId();
        }

        @Override
        public boolean closed() {
            return closed.get();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                registry.remove(identity);
            }
        }

    }

}
