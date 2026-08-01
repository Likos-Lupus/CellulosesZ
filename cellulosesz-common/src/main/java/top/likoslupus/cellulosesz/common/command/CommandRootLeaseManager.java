package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.validation.Checks;

import java.util.*;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Owns and transactionally replaces only the dispatcher roots registered by CellulosesZ.
 */
public final class CommandRootLeaseManager {

    private final CellulosesZLogger logger;
    private final CommandRootMutator mutator;
    private final Map<String, CommandNode<CommandSourceStack>> originalRoots = new LinkedHashMap<>();
    private final Map<String, Lease> leases = new LinkedHashMap<>();
    private long generation;
    private @Nullable CommandDispatcher<CommandSourceStack> dispatcher;

    public CommandRootLeaseManager(
            CellulosesZLogger logger,
            CommandRootMutator mutator
    ) {
        this.logger = requireNonNull(logger, "logger");
        this.mutator = requireNonNull(mutator, "mutator");
    }

    public synchronized void capture(CommandDispatcher<CommandSourceStack> dispatcher) {
        this.dispatcher = requireNonNull(dispatcher, "dispatcher");
        originalRoots.clear();
        dispatcher.getRoot().getChildren()
                .forEach(node ->
                        originalRoots.put(normalize(node.getName()), node)
                );
        leases.clear();
        generation++;
    }

    private String normalize(String value) {
        return Checks.requireNonBlank(value, "label").trim().toLowerCase(Locale.ROOT);
    }

    public synchronized BuildTransaction beginBuild() {
        requireDispatcher();
        var previous = new LinkedHashMap<>(leases);
        releaseOwned();
        generation++;
        return new BuildTransaction(this, previous);
    }

    private CommandDispatcher<CommandSourceStack> requireDispatcher() {
        return requireNonNull(dispatcher, "Command dispatcher has not been captured");
    }

    public synchronized void releaseOwned() {
        var root = requireDispatcher().getRoot();
        List.copyOf(leases.values()).stream()
                .filter(lease -> root.getChild(lease.label()) == lease.node())
                .forEach(lease -> {
                    mutator.remove(root, lease.label());
                    var original = originalRoots.get(lease.label());
                    if (original != null) {
                        root.addChild(original);
                    }
                });
        leases.clear();
    }

    public synchronized CommandNode<CommandSourceStack> claimCanonical(
            String label,
            String canonical,
            String owner,
            LabelKind kind,
            LiteralArgumentBuilder<CommandSourceStack> builder
    ) {
        var normalized = normalize(label);
        var existing = leases.get(normalized);

        if (existing != null) {
            throw new IllegalStateException(
                    "CellulosesZ command root conflict for /%s: %s (%s) vs %s (%s)".formatted(
                            normalized,
                            existing.canonical(),
                            existing.owner(),
                            canonical,
                            owner
                    ));
        }

        var root = requireDispatcher().getRoot();
        var current = root.getChild(normalized);
        var original = originalRoots.get(normalized);

        if (current != null && current != original) {
            throw new IllegalStateException(
                    "Cannot claim CellulosesZ command root /%s because another owner replaced it after dispatcher capture"
                            .formatted(normalized)
            );
        }

        if (current != null) {
            mutator.remove(root, normalized);
        }

        var node = requireDispatcher().register(
                requireNonNull(builder, "builder")
        );
        leases.put(
                normalized,
                new Lease(
                        normalized,
                        normalize(canonical),
                        Checks.requireNonBlank(owner, "owner"),
                        kind,
                        node,
                        generation
                )
        );

        return node;
    }

    public synchronized boolean claimAlias(
            String label,
            String canonical,
            String owner,
            LiteralArgumentBuilder<CommandSourceStack> builder
    ) {
        var normalized = normalize(label);
        if (leases.containsKey(normalized)
                || requireDispatcher().getRoot().getChild(normalized) != null
        ) {
            logger.warn(
                    "Skipping configured command alias /%s for /%s because the label is already owned".formatted(
                            normalized,
                            canonical
                    )
            );
            return false;
        }
        var node = requireDispatcher().register(requireNonNull(builder, "builder"));
        leases.put(
                normalized,
                new Lease(
                        normalized,
                        normalize(canonical),
                        Checks.requireNonBlank(owner, "owner"),
                        LabelKind.CONFIG_ALIAS,
                        node,
                        generation
                )
        );
        return true;
    }

    public synchronized Optional<CommandNode<CommandSourceStack>> ownedNode(String label) {
        return Optional.ofNullable(leases.get(normalize(label)))
                .map(Lease::node);
    }

    public synchronized Optional<Lease> lease(String label) {
        return Optional.ofNullable(leases.get(normalize(label)));
    }

    public synchronized int ownedCount() {
        return leases.size();
    }

    public synchronized long generation() {
        return generation;
    }

    private synchronized void rollback(Map<String, Lease> previous) {
        releaseOwned();
        var root = requireDispatcher().getRoot();

        previous.values().forEach(lease -> {
            var current = root.getChild(lease.label());
            var original = originalRoots.get(lease.label());

            if (current != null && current != original) {
                logger.warn(
                        "Cannot restore CellulosesZ command root /%s because another owner replaced it during rebuild".formatted(
                                lease.label()
                        )
                );
                return;
            }

            if (current != null) {
                mutator.remove(root, lease.label());
            }
            root.addChild(lease.node());
            leases.put(lease.label(), lease);
        });

        generation++;
    }

    public enum LabelKind {

        CANONICAL,
        SEMANTIC_ROOT,
        CONFIG_ALIAS,
        DECLARED_ALIAS

    }

    public record Lease(
            String label,
            String canonical,
            String owner,
            LabelKind kind,
            CommandNode<CommandSourceStack> node,
            long generation
    ) {

    }

    public static final class BuildTransaction implements AutoCloseable {

        private final CommandRootLeaseManager manager;
        private final Map<String, Lease> previous;
        private boolean committed;

        private BuildTransaction(
                CommandRootLeaseManager manager,
                Map<String, Lease> previous
        ) {
            this.manager = manager;
            this.previous = previous;
        }

        public void commit() {
            committed = true;
        }

        @Override
        public void close() {
            if (!committed) {
                manager.rollback(previous);
            }
        }

    }

}
