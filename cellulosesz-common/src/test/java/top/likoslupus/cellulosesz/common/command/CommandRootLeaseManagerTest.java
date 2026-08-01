package top.likoslupus.cellulosesz.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class CommandRootLeaseManagerTest {

    private final RecordingLogger logger = new RecordingLogger();
    private final CommandRootLeaseManager leases = new CommandRootLeaseManager(logger, CommandRootLeaseManagerTest::remove);
    private final CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();

    private static CommandRootLeaseManager.LabelKind semantic() {
        return CommandRootLeaseManager.LabelKind.SEMANTIC_ROOT;
    }

    @Test
    void directCanonicalConflictFailsFast() {
        leases.capture(dispatcher);
        leases.claimCanonical(
                "info",
                "info",
                "text",
                canonical(),
                literal("info")
        );
        assertThrows(
                IllegalStateException.class,
                () -> leases.claimCanonical(
                        "info",
                        "motd",
                        "text",
                        canonical(),
                        literal("info")
                )
        );
    }

    private static CommandRootLeaseManager.LabelKind canonical() {
        return CommandRootLeaseManager.LabelKind.CANONICAL;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return LiteralArgumentBuilder.literal(name);
    }

    @Test
    void configuredAliasConflictSkipsAndWarns() {
        dispatcher.register(literal("rules"));
        leases.capture(dispatcher);

        assertFalse(leases.claimAlias(
                "rules",
                "info",
                "config",
                literal("rules")
        ));
        assertTrue(logger.warnings.stream()
                .anyMatch(message -> message.contains("/rules"))
        );
    }

    @Test
    void releaseRestoresVanillaAndProtectsForeignReplacement() {
        var vanilla = dispatcher.register(literal("kill"));
        leases.capture(dispatcher);
        leases.claimCanonical(
                "kill",
                "kill",
                "admin",
                canonical(),
                literal("kill")
        );
        leases.releaseOwned();
        assertSame(vanilla, dispatcher.getRoot().getChild("kill"));

        leases.claimCanonical(
                "kit",
                "kit",
                "kit",
                canonical(),
                literal("kit")
        );
        remove(dispatcher.getRoot(), "kit");

        var foreign = dispatcher
                .register(literal("kit")
                        .then(literal("foreign")));
        leases.releaseOwned();
        assertSame(foreign, dispatcher.getRoot().getChild("kit"));
    }

    @SuppressWarnings("unchecked")
    private static void remove(CommandNode<CommandSourceStack> root, String label) {
        List.of("children", "literals", "arguments")
                .forEach(fieldName -> {
                    try {
                        var field = CommandNode.class.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        ((Map<String, ?>) field.get(root)).remove(label);
                    } catch (ReflectiveOperationException exception) {
                        throw new AssertionError(exception);
                    }
                });
    }

    @Test
    void failedBuildRollsBackWholePreviousTree() {
        leases.capture(dispatcher);
        var previous = leases.claimCanonical(
                "home",
                "home",
                "home",
                canonical(),
                literal("home").then(literal("old"))
        );

        assertThrows(
                IllegalStateException.class,
                () -> {
                    try (var transaction = leases.beginBuild()) {
                        leases.claimCanonical(
                                "home",
                                "home",
                                "home",
                                canonical(),
                                literal("home").then(literal("new"))
                        );
                        leases.claimCanonical(
                                "home",
                                "warp",
                                "warp",
                                canonical(),
                                literal("home")
                        );
                        transaction.commit();
                    }
                }
        );
        assertSame(previous, dispatcher.getRoot().getChild("home"));
    }

    @Test
    void rebuildNeverDeletesAForeignReplacement() {
        leases.capture(dispatcher);
        leases.claimCanonical(
                "kit",
                "kit",
                "kit",
                canonical(),
                literal("kit")
        );
        remove(dispatcher.getRoot(), "kit");
        var foreign = dispatcher.register(
                literal("kit").then(literal("foreign"))
        );

        assertThrows(
                IllegalStateException.class,
                () -> {
                    try (var transaction = leases.beginBuild()) {
                        leases.claimCanonical(
                                "kit",
                                "kit",
                                "kit",
                                canonical(),
                                literal("kit")
                        );
                        transaction.commit();
                    }
                }
        );

        assertSame(foreign, dispatcher.getRoot().getChild("kit"));
        assertTrue(logger.warnings.stream()
                .anyMatch(message -> message.contains("another owner"))
        );
    }

    @Test
    void dispatcherGenerationChangesOnCaptureAndBuild() {
        leases.capture(dispatcher);

        var captured = leases.generation();
        try (var transaction = leases.beginBuild()) {
            transaction.commit();
        }

        assertTrue(leases.generation() > captured);

        var other = new CommandDispatcher<CommandSourceStack>();
        leases.capture(other);
        assertEquals(0, leases.ownedCount());
    }

    @NullMarked
    private static final class RecordingLogger implements CellulosesZLogger {

        private final List<String> warnings = new ArrayList<>();

        @Override
        public void warn(String message) {
            warnings.add(message);
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void info(String message) {
        }

    }

}
