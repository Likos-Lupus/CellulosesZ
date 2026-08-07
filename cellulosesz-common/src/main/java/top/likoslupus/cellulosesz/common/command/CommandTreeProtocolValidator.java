package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;

import java.util.*;

/** Validates that owned Brigadier argument nodes are serializable by the vanilla command protocol. */
final class CommandTreeProtocolValidator {

    void validate(List<CommandRootLeaseManager.Lease> leases) {
        var visited = Collections.newSetFromMap(
                new IdentityHashMap<CommandNode<CommandSourceStack>, Boolean>()
        );
        var failures = new ArrayList<Failure>();

        List.copyOf(leases).forEach(lease ->
                traverse(lease, visited, failures)
        );

        if (!failures.isEmpty()) {
            throw new IllegalStateException(message(failures));
        }
    }

    private void traverse(
            CommandRootLeaseManager.Lease lease,
            Set<CommandNode<CommandSourceStack>> visited,
            List<Failure> failures
    ) {
        var pending = new ArrayDeque<PathNode>();
        pending.add(new PathNode(lease.node(), "/" + lease.label()));

        while (!pending.isEmpty()) {
            var current = pending.removeFirst();
            if (!visited.add(current.node())) {
                continue;
            }

            if (current.node() instanceof ArgumentCommandNode<CommandSourceStack, ?> argument) {
                try {
                    ArgumentTypeInfos.unpack(argument.getType());
                } catch (RuntimeException exception) {
                    failures.add(new Failure(
                            lease.label(),
                            current.path(),
                            argument.getName(),
                            argument.getType().getClass().getName(),
                            lease.owner(),
                            lease.canonical(),
                            exception.getMessage() == null
                                    ? exception.getClass().getName()
                                    : exception.getMessage()
                    ));
                }
            }

            current.node().getChildren().stream()
                    .map(child -> new PathNode(
                            child,
                            current.path() + " " + segment(child)
                    ))
                    .forEach(pending::addLast);

            var redirect = current.node().getRedirect();
            if (redirect != null) {
                pending.addLast(new PathNode(redirect, current.path()));
            }
        }
    }

    private static String message(List<Failure> failures) {
        var result = new StringBuilder()
                .append("CellulosesZ command tree is not client-serializable (")
                .append(failures.size())
                .append(" argument nodes):\n");
        failures.forEach(failure -> result
                .append("\n- ").append(failure.path()).append('\n')
                .append("  root: ").append(failure.root()).append('\n')
                .append("  owner: ").append(failure.owner()).append('\n')
                .append("  canonical: ").append(failure.canonical()).append('\n')
                .append("  argument: ").append(failure.argument()).append('\n')
                .append("  type: ").append(failure.type()).append('\n')
                .append("  reason: ").append(failure.reason()).append('\n'));
        return result.toString();
    }

    private static String segment(CommandNode<CommandSourceStack> node) {
        if (node instanceof ArgumentCommandNode<CommandSourceStack, ?>) {
            return "<" + node.getName() + ">";
        }
        return node.getName();
    }

    private record PathNode(
            CommandNode<CommandSourceStack> node,
            String path
    ) {

    }

    private record Failure(
            String root,
            String path,
            String argument,
            String type,
            String owner,
            String canonical,
            String reason
    ) {

    }

}
