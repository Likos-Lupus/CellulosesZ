package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import top.likoslupus.cellulosesz.api.command.service.*;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Guarded server-thread dispatch for explicitly permitted indirect command origins.
 */
public final class FabricPlayerCommandDispatchService implements PlayerCommandDispatchService {

    private static final int MAX_COMMAND_LENGTH = 2_048;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_INDIRECT_EXECUTIONS_PER_TICK = 64;

    private final FabricServerAccess access;
    private final ThreadLocal<ArrayDeque<Frame>> chain = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<UUID, Integer> tickExecutions = new HashMap<>();

    public FabricPlayerCommandDispatchService(FabricServerAccess access) {
        this.access = requireNonNull(access, "access");
    }

    @Override
    public PlayerCommandDispatchResult dispatch(PlayerCommandDispatchRequest request) {
        requireNonNull(request, "request");
        var server = access.requireServer();
        if (!server.isSameThread()) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    "Player command dispatch requires the server thread"
            );
        }

        var normalized = normalize(request.command());
        if (normalized.isEmpty()) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    "Command is blank"
            );
        }

        if (normalized.length() > MAX_COMMAND_LENGTH) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    "Command exceeds the maximum length of " + MAX_COMMAND_LENGTH
            );
        }

        if (containsControl(normalized)) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    "Command contains control characters"
            );
        }

        var stack = chain.get();
        if (stack.size() >= MAX_DEPTH) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    "Maximum indirect command depth exceeded"
            );
        }

        var targetExecutions = (int) tickExecutions.getOrDefault(
                request.target().uuid(),
                0
        );

        if (targetExecutions >= MAX_INDIRECT_EXECUTIONS_PER_TICK) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    "Maximum indirect commands per tick exceeded"
            );
        }

        var frame = new Frame(
                request.actorId(),
                request.target().uuid(),
                root(normalized),
                request.origin(),
                request.chainToken()
        );
        var guardFailure = guardFailure(stack, frame);

        if (guardFailure.isPresent()) {
            return failure(
                    CommandDispatchStatus.REJECTED_BY_GUARD,
                    guardFailure.orElseThrow()
            );
        }

        var nativePlayer = access.player(request.target());
        stack.push(frame);
        tickExecutions.put(request.target().uuid(), targetExecutions + 1);

        try {
            var result = server.getCommands()
                    .getDispatcher()
                    .execute(normalized, nativePlayer.createCommandSourceStack());
            return PlayerCommandDispatchResult.executed(result);
        } catch (CommandSyntaxException exception) {
            return failure(CommandDispatchStatus.SYNTAX_ERROR, syntaxDetail(exception));
        } catch (RuntimeException exception) {
            return failure(
                    CommandDispatchStatus.INTERNAL_ERROR,
                    exception.getClass().getSimpleName()
            );
        } finally {
            stack.pop();
            if (stack.isEmpty()) {
                chain.remove();
            }
        }
    }

    @Override
    public void beginTick() {
        tickExecutions.clear();
    }

    private static PlayerCommandDispatchResult failure(
            CommandDispatchStatus status,
            String detail
    ) {
        return PlayerCommandDispatchResult.failure(status, detail);
    }

    private static String normalize(String command) {
        var normalized = command.strip();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.strip();
    }

    private static boolean containsControl(String value) {
        return value.codePoints().anyMatch(codePoint ->
                codePoint == 0 || Character.isISOControl(codePoint)
        );
    }

    private static String root(String command) {
        var split = command.indexOf(' ');
        return (
                split < 0
                        ? command
                        : command.substring(0, split)
        ).toLowerCase(Locale.ROOT);
    }

    private static Optional<String> guardFailure(
            ArrayDeque<Frame> chain,
            Frame candidate
    ) {
        Set<UUID> actors = new HashSet<>();
        for (var frame : chain) {
            if (frame.equals(candidate)) {
                return Optional.of("Recursive command dispatch was blocked");
            }

            if (frame.actor().equals(candidate.target())
                    && frame.target().equals(candidate.actor())
            ) {
                return Optional.of("Mutual sudo or command-dispatch loop was blocked");
            }

            if (frame.target().equals(candidate.target())
                    && frame.root().equals(candidate.root())
            ) {
                return Optional.of("Repeated command root in one target chain was blocked");
            }

            if (candidate.origin() == CommandDispatchOrigin.PREPROCESS_REWRITE
                    && frame.origin() == CommandDispatchOrigin.PREPROCESS_REWRITE
                    && frame.target().equals(candidate.target())
            ) {
                return Optional.of("Command preprocess rewrite re-entry was blocked");
            }

            actors.add(frame.actor());
        }

        if (actors.contains(candidate.target())
                && chain.stream().anyMatch(frame -> frame.root().equals(candidate.root()))
        ) {
            return Optional.of("PowerTool and sudo command cycle was blocked");
        }

        return Optional.empty();
    }

    private static String syntaxDetail(CommandSyntaxException exception) {
        return exception.getRawMessage().getString();
    }

    private record Frame(
            UUID actor,
            UUID target,
            String root,
            CommandDispatchOrigin origin,
            UUID token
    ) {

    }

}
