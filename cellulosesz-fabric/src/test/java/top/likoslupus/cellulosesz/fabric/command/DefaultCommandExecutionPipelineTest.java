package top.likoslupus.cellulosesz.fabric.command;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.BanRecord;
import top.likoslupus.cellulosesz.api.admin.MuteService;
import top.likoslupus.cellulosesz.api.command.CommandMiddleware;
import top.likoslupus.cellulosesz.api.command.CommandRegistry;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CommandCostService;
import top.likoslupus.cellulosesz.api.config.ConfigRegistry;
import top.likoslupus.cellulosesz.api.event.EventRegistry;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.ModuleContext;
import top.likoslupus.cellulosesz.api.scheduler.Scheduler;
import top.likoslupus.cellulosesz.api.service.Registration;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.core.command.execution.DefaultCommandExecutionPipeline;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.service.MuteCommandMiddleware;
import top.likoslupus.cellulosesz.modules.command.middleware.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultCommandExecutionPipelineTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void sourceKindAndPermissionBlockBeforeTerminal() {
        var logger = new RecordingLogger();
        var pipeline = new DefaultCommandExecutionPipeline(logger);

        pipeline.addMiddleware(new SourceKindCommandMiddleware());
        pipeline.addMiddleware(new PermissionCommandMiddleware());

        var terminal = new AtomicInteger();
        var console = new TestPolicy(
                false,
                false,
                "home",
                "home"
        );
        assertEquals(
                0,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        console,
                        terminal::incrementAndGet
                )
        );
        assertEquals(0, terminal.get());

        var playerWithoutPermission = new TestPolicy(
                true,
                false,
                "home",
                "home"
        );
        assertEquals(
                0,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        playerWithoutPermission,
                        terminal::incrementAndGet
                )
        );
        assertEquals(0, terminal.get());

        var player = new TestPolicy(
                true,
                true,
                "home",
                "home"
        );
        assertEquals(
                1,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        player,
                        terminal::incrementAndGet
                )
        );
        assertEquals(1, terminal.get());

        var playerForConsoleOnly = new TestPolicy(
                true,
                true,
                "gc",
                "gc"
        );
        assertEquals(
                0,
                pipeline.execute(
                        new CommandDescriptor(
                                "command",
                                "gc",
                                "cellulosesz.home.use",
                                CommandSourceKind.CONSOLE_ONLY
                        ),
                        playerForConsoleOnly,
                        terminal::incrementAndGet
                )
        );
        assertEquals(1, terminal.get());
    }

    private static CommandDescriptor descriptor(CommandSourceKind kind) {
        return new CommandDescriptor(
                "home",
                "home",
                "cellulosesz.home.use",
                kind
        );
    }

    @Test
    void disabledModuleAndMuteBlockTerminal() {
        var logger = new RecordingLogger();
        var pipeline = new DefaultCommandExecutionPipeline(logger);
        pipeline.addMiddleware(new ModuleEnabledCommandMiddleware(moduleContext(false)));

        var terminal = new AtomicInteger();
        assertEquals(
                0,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        policy(),
                        terminal::incrementAndGet
                )
        );
        assertEquals(0, terminal.get());

        var mutedPipeline = new DefaultCommandExecutionPipeline(logger);
        var config = new AdminConfig();
        config.muteCommands = Set.of("home");
        mutedPipeline.addMiddleware(new MuteCommandMiddleware(muteService(true), config));

        assertEquals(
                0,
                mutedPipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        new TestPolicy(
                                true,
                                false,
                                "home",
                                "home"
                        ),
                        terminal::incrementAndGet
                )
        );
        assertEquals(0, terminal.get());
    }

    @NullMarked
    private static ModuleContext moduleContext(boolean enabled) {
        return new ModuleContext() {
            @Override
            public String moduleId() {
                return "command";
            }

            @Override
            public Path dataDirectory() {
                return Path.of(".");
            }

            @Override
            public ServiceRegistry services() {
                return null;
            }

            @Override
            public ConfigRegistry configs() {
                return null;
            }

            @Override
            public EventRegistry events() {
                return null;
            }

            @Override
            public CommandRegistry commands() {
                return null;
            }

            @Override
            public Scheduler scheduler() {
                return null;
            }

            @Override
            public CellulosesZLogger logger() {
                return new RecordingLogger();
            }

            @Override
            public boolean moduleEnabled(String moduleId) {
                return enabled;
            }

            @Override
            public void track(Registration registration) {
            }
        };
    }

    private static TestPolicy policy() {
        return new TestPolicy(
                true,
                true,
                "home",
                "home"
        );
    }

    @NullMarked
    private static MuteService muteService(boolean muted) {
        return new MuteService() {
            @Override
            public CompletableFuture<AdminResult> mute(
                    UUID uuid,
                    String name,
                    String actor,
                    @Nullable Long durationMillis,
                    String reason
            ) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CompletableFuture<AdminResult> unmute(UUID uuid, String name, String actor) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean muted(UUID uuid) {
                return muted;
            }

            @Override
            public Optional<BanRecord> record(UUID uuid) {
                return Optional.empty();
            }

            @Override
            public CompletableFuture<Integer> purgeExpired() {
                return CompletableFuture.completedFuture(0);
            }
        };
    }

    @Test
    void commandCostContinuesExactlyOnceOnServerThreadAfterSuccessfulCharge() {
        var terminal = new AtomicInteger();
        var executor = new ImmediateServerThreadExecutor();
        var costs = new TestCosts(true);
        var pipeline = new DefaultCommandExecutionPipeline(new RecordingLogger());
        pipeline.addMiddleware(new CommandCostMiddleware(costs, executor));

        assertEquals(
                1,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        policy(),
                        terminal::incrementAndGet
                )
        );
        assertEquals(1, costs.charges.get());
        assertEquals(1, terminal.get());
        assertEquals(1, executor.executions.get());
    }

    @Test
    void failedChargeDoesNotExecuteTerminal() {
        var terminal = new AtomicInteger();
        var policy = policy();
        var pipeline = new DefaultCommandExecutionPipeline(new RecordingLogger());
        pipeline.addMiddleware(new CommandCostMiddleware(new TestCosts(false), new ImmediateServerThreadExecutor()));

        assertEquals(
                1,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        policy,
                        terminal::incrementAndGet
                )
        );
        assertEquals(0, terminal.get());
        assertFalse(policy.errors.isEmpty());
    }

    @Test
    void auditUsesCanonicalRootAndSafeSummary() {
        var logger = new RecordingLogger();
        var pipeline = new DefaultCommandExecutionPipeline(logger);
        pipeline.addMiddleware(new AuditCommandMiddleware(logger));
        var context = new TestPolicy(
                true,
                true,
                "h",
                "home"
        );
        context.summary = "arguments=1 redacted=true";

        assertEquals(
                7,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        context,
                        () -> 7
                )
        );
        assertTrue(
                logger.infos.stream()
                        .anyMatch(message ->
                                message.contains("/h")
                                        && message.contains("canonical=home")
                                        && message.contains("redacted=true")
                        )
        );
    }

    @Test
    void middlewareExceptionIsIsolatedAndReported() {
        var logger = new RecordingLogger();
        var pipeline = new DefaultCommandExecutionPipeline(logger);
        pipeline.addMiddleware((_, _, _) -> {
            throw new IllegalStateException("boom");
        });
        var context = policy();

        assertEquals(
                0,
                pipeline.execute(
                        descriptor(CommandSourceKind.PLAYER_ONLY),
                        context,
                        () -> 1
                )
        );
        assertEquals(1, logger.failures.size());
        assertFalse(context.errors.isEmpty());
    }

    @Test
    void directAndLegacyContextsTraverseIdenticalMiddlewareOrder() {
        var order = new ArrayList<String>();
        var pipeline = new DefaultCommandExecutionPipeline(new RecordingLogger());
        pipeline.addMiddleware(mark("source", order));
        pipeline.addMiddleware(mark("module", order));
        pipeline.addMiddleware(mark("permission", order));
        pipeline.addMiddleware(mark("cost", order));
        pipeline.addMiddleware(mark("mute", order));
        pipeline.addMiddleware(mark("audit", order));

        assertEquals(
                1,
                pipeline.execute(
                        descriptor(CommandSourceKind.ANY),
                        new TestPolicy(
                                true,
                                true,
                                "home",
                                "home"
                        ),
                        () -> {
                            order.add("terminal");
                            return 1;
                        }
                )
        );
        var directOrder = List.copyOf(order);
        order.clear();
        assertEquals(
                1,
                pipeline.execute(
                        descriptor(CommandSourceKind.ANY),
                        new TestPolicy(
                                true,
                                true,
                                "legacy-alias",
                                "home"
                        ),
                        () -> {
                            order.add("terminal");
                            return 1;
                        }
                )
        );
        assertEquals(directOrder, order);
    }

    private static CommandMiddleware mark(String name, List<String> order) {
        return (_, _, continuation) -> {
            order.add(name);
            return continuation.proceed();
        };
    }

    @NullMarked
    private static final class TestCosts implements CommandCostService {

        private final boolean charged;
        private final AtomicInteger charges = new AtomicInteger();

        private TestCosts(boolean charged) {
            this.charged = charged;
        }

        @Override
        public BigDecimal cost(String command) {
            return BigDecimal.ONE;
        }

        @Override
        public CompletableFuture<Boolean> charge(UUID uuid, String command) {
            charges.incrementAndGet();
            return CompletableFuture.completedFuture(charged);
        }

    }

    @NullMarked
    private static final class ImmediateServerThreadExecutor implements ServerThreadExecutor {

        private final AtomicInteger executions = new AtomicInteger();

        @Override
        public boolean isServerThread() {
            return true;
        }

        @Override
        public void execute(Runnable task) {
            executions.incrementAndGet();
            task.run();
        }

        @Override
        public <T> CompletableFuture<T> submit(Supplier<T> task) {
            return CompletableFuture.completedFuture(task.get());
        }

    }

    @NullMarked
    private static final class TestPolicy implements CommandPolicyContext {

        private final boolean player;
        private final boolean permission;
        private final String invoked;
        private final String canonical;
        private final List<LocalizedMessage> errors = new ArrayList<>();
        private String summary = "arguments=0";

        private TestPolicy(
                boolean player,
                boolean permission,
                String invoked,
                String canonical
        ) {
            this.player = player;
            this.permission = permission;
            this.invoked = invoked;
            this.canonical = canonical;
        }

        @Override
        public String invokedLabel() {
            return invoked;
        }

        @Override
        public String canonicalRoot() {
            return canonical;
        }

        @Override
        public boolean player() {
            return player;
        }

        @Override
        public Optional<UUID> playerUuid() {
            return player
                    ? Optional.of(PLAYER)
                    : Optional.empty();
        }

        @Override
        public Optional<String> playerName() {
            return player
                    ? Optional.of("Tester")
                    : Optional.empty();
        }

        @Override
        public boolean hasPermission(String permission) {
            return this.permission;
        }

        @Override
        public String auditSummary() {
            return summary;
        }

        @Override
        public void reply(LocalizedMessage message) {
        }

        @Override
        public void error(LocalizedMessage message) {
            errors.add(message);
        }

    }

    @NullMarked
    private static final class RecordingLogger implements CellulosesZLogger {

        private final List<String> infos = new ArrayList<>();
        private final List<Throwable> failures = new ArrayList<>();

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
            failures.add(throwable);
        }

        @Override
        public void info(String message) {
            infos.add(message);
        }

    }

}
