package top.likoslupus.cellulosesz.fabric;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import top.likoslupus.cellulosesz.api.command.service.CommandDispatchOrigin;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchResult;
import top.likoslupus.cellulosesz.api.command.service.PlayerCommandDispatchService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;

import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * Server-thread command dispatch with one shared recursion budget for sudo and PowerTool execution.
 */
public final class FabricPlayerCommandDispatchService implements PlayerCommandDispatchService {

    private static final int MAX_DEPTH = 8;
    private static final int MAX_INDIRECT_EXECUTIONS_PER_TICK = 64;

    private final FabricPlatformService platform;
    private final ThreadLocal<ArrayDeque<Frame>> chain = ThreadLocal.withInitial(ArrayDeque::new);
    private final Map<UUID, Integer> tickExecutions = new HashMap<>();

    public FabricPlayerCommandDispatchService(FabricPlatformService platform) {
        this.platform = requireNonNull(platform, "platform");
    }

    @Override
    public PlayerCommandDispatchResult dispatch(
            CellPlayer player,
            String command,
            CommandDispatchOrigin origin
    ) {
        requireNonNull(player, "player");
        requireNonNull(command, "command");
        requireNonNull(origin, "origin");

        var server = platform.requireServer();
        if (!server.isSameThread()) {
            return failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Player command dispatch requires the server thread");
        }

        var normalized = normalize(command);
        if (normalized.isEmpty()) {
            return failure(PlatformOperationStatus.INVALID_ARGUMENT, "Command is blank");
        }
        if (containsControl(normalized)) {
            return failure(PlatformOperationStatus.INVALID_ARGUMENT, "Command contains control characters");
        }

        var stack = chain.get();
        if (stack.size() >= MAX_DEPTH) {
            return failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Maximum indirect command depth exceeded");
        }

        var frame = new Frame(player.uuid(), root(normalized), origin);
        if (containsCycle(stack, frame)) {
            return failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Recursive command dispatch was blocked");
        }

        var executions = tickExecutions.merge(player.uuid(), 1, Integer::sum);
        if (executions > MAX_INDIRECT_EXECUTIONS_PER_TICK) {
            return failure(PlatformOperationStatus.STATE_NOT_ALLOWED, "Per-tick indirect command budget exceeded");
        }

        stack.push(frame);
        try {
            var result = server.getCommands().getDispatcher().execute(
                    normalized,
                    platform.nativePlayer(player).createCommandSourceStack()
            );
            return result > 0
                    ? new PlayerCommandDispatchResult(PlatformOperationStatus.SUCCESS, result, "")
                    : new PlayerCommandDispatchResult(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            result,
                            "Command returned a non-success result"
                    );
        } catch (CommandSyntaxException failure) {
            return new PlayerCommandDispatchResult(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    0,
                    failure.getRawMessage().getString()
            );
        } catch (RuntimeException failure) {
            return new PlayerCommandDispatchResult(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    0,
                    failure.getClass().getSimpleName()
            );
        } finally {
            stack.pop();
            if (stack.isEmpty()) chain.remove();
        }
    }

    @Override
    public void beginTick() {
        tickExecutions.clear();
    }

    private static PlayerCommandDispatchResult failure(PlatformOperationStatus status, String detail) {
        return new PlayerCommandDispatchResult(status, 0, detail);
    }

    private static String normalize(String command) {
        var normalized = command.strip();
        if (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized.strip();
    }

    private static boolean containsControl(String value) {
        return value.codePoints().anyMatch(codePoint -> codePoint == 0 || Character.isISOControl(codePoint));
    }

    private static String root(String command) {
        var split = command.indexOf(' ');
        return (split < 0 ? command : command.substring(0, split)).toLowerCase(Locale.ROOT);
    }

    private static boolean containsCycle(ArrayDeque<Frame> chain, Frame candidate) {
        Set<UUID> players = new HashSet<>();
        for (var frame : chain) {
            if (frame.equals(candidate)) return true;
            players.add(frame.player());
        }
        return players.contains(candidate.player()) && chain.stream()
                .anyMatch(frame -> frame.root().equals(candidate.root()));
    }

    private record Frame(
            UUID player,
            String root,
            CommandDispatchOrigin origin
    ) {

    }

}
