package top.likoslupus.cellulosesz.core.module;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.*;
import top.likoslupus.cellulosesz.core.command.execution.DefaultCommandExecutionPipeline;
import top.likoslupus.cellulosesz.core.config.JacksonConfigRegistry;
import top.likoslupus.cellulosesz.core.config.ModulesConfig;
import top.likoslupus.cellulosesz.core.event.SimpleEventRegistry;
import top.likoslupus.cellulosesz.core.scheduler.DefaultScheduler;
import top.likoslupus.cellulosesz.core.service.DefaultServiceRegistry;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultModuleManagerDiffTest {

    private static final List<String> LOG = new ArrayList<>();
    private final List<DefaultScheduler> schedulers = new ArrayList<>();
    @TempDir Path root;

    @BeforeEach
    void resetLog() {
        LOG.clear();
        FailingModule.closed = false;
        TransactionModuleA.mode = TransactionMode.SUCCESS;
        TransactionModuleB.mode = TransactionMode.SUCCESS;
        TransactionModuleA.prepareGate = null;
        TransactionModuleB.prepareGate = null;
    }

    @AfterEach
    void closeSchedulers() {
        schedulers.forEach(DefaultScheduler::close);
    }

    @Test
    void togglesLoadAndUnloadInDependencyOrder() {
        var fixture = fixture(List.of(
                descriptor(
                        "dependency",
                        List.of(),
                        List.of(),
                        true,
                        DependencyModule.class
                ),
                descriptor(
                        "dependent",
                        List.of("dependency"),
                        List.of(),
                        false,
                        DependentModule.class
                )
        ));
        fixture.manager.loadAsync().join();
        assertEquals(List.of("load:dependency"), LOG);

        LOG.clear();
        fixture.modules().modules.put("dependent", true);
        reload(fixture);
        assertEquals(List.of("load:dependent"), LOG);

        LOG.clear();
        fixture.modules().modules.put("dependent", false);
        fixture.modules().modules.put("dependency", false);
        reload(fixture);

        assertEquals(List.of("unload:dependent", "unload:dependency"), LOG);
        assertFalse(fixture.manager.moduleEnabled("dependency"));
        assertFalse(fixture.manager.moduleEnabled("dependent"));
    }

    private Fixture fixture(List<ModuleDescriptor> descriptors) {
        var logger = new NoopLogger();
        var services = new DefaultServiceRegistry();
        var configs = new JacksonConfigRegistry(
                root.resolve("config-" + schedulers.size()),
                logger
        );
        var scheduler = new DefaultScheduler(logger);

        schedulers.add(scheduler);
        var manager = new DefaultModuleManager(
                () -> descriptors,
                root.resolve("data-" + schedulers.size()),
                services,
                configs,
                new SimpleEventRegistry(),
                scheduler,
                new DefaultCommandExecutionPipeline(logger, services),
                logger
        );

        return new Fixture(manager, configs, services);
    }

    private static ModuleDescriptor descriptor(
            String id,
            List<String> requires,
            List<String> optional,
            boolean enabled,
            Class<? extends CellulosesZModule> type
    ) {
        return new ModuleDescriptor(
                id,
                id,
                id,
                ModulePhase.FEATURE,
                0,
                requires,
                optional,
                enabled,
                type
        );
    }

    private static void reload(Fixture fixture) {
        fixture.manager.prepareReload(fixture.configs.snapshot())
                .thenCompose(PreparedModuleReload::commit)
                .toCompletableFuture()
                .join();
    }

    @Test
    void invalidRequiredDependencyFailsBeforeChangingActiveModules() {
        var fixture = fixture(List.of(
                descriptor(
                        "dependency",
                        List.of(),
                        List.of(),
                        true,
                        DependencyModule.class
                ),
                descriptor(
                        "dependent",
                        List.of("dependency"),
                        List.of(),
                        true,
                        DependentModule.class
                )
        ));
        fixture.manager.loadAsync().join();

        LOG.clear();
        fixture.modules().modules.put("dependency", false);
        assertThrows(
                RuntimeException.class,
                () -> reload(fixture)
        );

        assertTrue(LOG.isEmpty());
        assertTrue(fixture.manager.moduleEnabled("dependency"));
        assertTrue(fixture.manager.moduleEnabled("dependent"));
    }

    @Test
    void optionalAvailabilityRestartsConsumerAndRequiredDependents() {
        var fixture = fixture(List.of(
                descriptor(
                        "provider",
                        List.of(),
                        List.of(),
                        false,
                        ProviderModule.class
                ),
                descriptor(
                        "consumer",
                        List.of(),
                        List.of("provider"),
                        true,
                        ConsumerModule.class
                ),
                descriptor(
                        "required-dependent",
                        List.of("consumer"),
                        List.of(),
                        true,
                        RequiredDependentModule.class
                )
        ));
        fixture.manager.loadAsync().join();

        LOG.clear();
        fixture.modules().modules.put("provider", true);
        reload(fixture);

        assertEquals(
                List.of(
                        "unload:required-dependent",
                        "unload:consumer",
                        "load:provider",
                        "load:consumer",
                        "load:required-dependent"
                ),
                LOG
        );
    }

    @Test
    void failedLoadClosesEveryScopedSideEffect() {
        var fixture = fixture(List.of(
                descriptor(
                        "failing",
                        List.of(),
                        List.of(),
                        true,
                        FailingModule.class
                )
        ));

        assertThrows(
                RuntimeException.class,
                () -> fixture.manager.loadAsync().join()
        );
        assertFalse(fixture.services.contains(MarkerService.class));
        assertTrue(FailingModule.closed);
        assertFalse(fixture.manager.moduleEnabled("failing"));
    }

    @Test
    void prepareFailureRollsBackEarlierPreparedModulesWithoutCommitting() {
        var fixture = transactionFixture();
        fixture.manager.loadAsync().join();
        LOG.clear();
        TransactionModuleB.mode = TransactionMode.PREPARE_FAILURE;

        assertThrows(
                RuntimeException.class,
                () -> fixture.manager.prepareReload(fixture.configs.snapshot())
                        .toCompletableFuture()
                        .join()
        );

        assertEquals(List.of("prepare:a", "prepare:b", "rollback:a"), LOG);
    }

    private Fixture transactionFixture() {
        return fixture(List.of(
                descriptor(
                        "a",
                        List.of(),
                        List.of(),
                        true,
                        TransactionModuleA.class
                ),
                descriptor(
                        "b",
                        List.of("a"),
                        List.of(),
                        true,
                        TransactionModuleB.class
                )
        ));
    }

    @Test
    void commitFailureRollsBackPreparedModulesInReverseOrder() {
        var fixture = transactionFixture();
        fixture.manager.loadAsync().join();
        LOG.clear();
        TransactionModuleB.mode = TransactionMode.COMMIT_FAILURE;

        var prepared = fixture.manager.prepareReload(fixture.configs.snapshot())
                .toCompletableFuture()
                .join();
        assertThrows(
                RuntimeException.class,
                () -> prepared.commit().toCompletableFuture().join()
        );
        prepared.rollback().toCompletableFuture().join();

        assertEquals(
                List.of(
                        "prepare:a",
                        "prepare:b",
                        "commit:a",
                        "commit:b",
                        "rollback:b",
                        "rollback:a"
                ),
                LOG
        );
    }

    @Test
    void rollbackFailureDoesNotPreventRemainingRollbackAndIsAggregated() {
        var fixture = transactionFixture();
        fixture.manager.loadAsync().join();
        LOG.clear();
        TransactionModuleB.mode = TransactionMode.ROLLBACK_FAILURE;

        var prepared = fixture.manager.prepareReload(fixture.configs.snapshot())
                .toCompletableFuture()
                .join();
        prepared.commit().toCompletableFuture().join();

        var failure = assertThrows(
                RuntimeException.class,
                () -> prepared.rollback().toCompletableFuture().join()
        );

        assertEquals(
                List.of(
                        "prepare:a",
                        "prepare:b",
                        "commit:a",
                        "commit:b",
                        "rollback:b",
                        "rollback:a"
                ),
                LOG
        );
        assertTrue(failure.getCause().getSuppressed().length > 0);
    }

    @Test
    void delayedModulePreparationKeepsManagerReloadIncomplete() {
        var fixture = transactionFixture();
        fixture.manager.loadAsync().join();
        LOG.clear();
        var gate = new CompletableFuture<Void>();
        TransactionModuleB.prepareGate = gate;

        var preparation = fixture.manager.prepareReload(fixture.configs.snapshot())
                .toCompletableFuture();

        assertFalse(preparation.isDone());
        gate.complete(null);
        var prepared = preparation.join();
        prepared.rollback().toCompletableFuture().join();
        assertEquals(
                List.of("prepare:a", "prepare:b", "rollback:b", "rollback:a"),
                LOG
        );
    }

    @Test
    void failedToggleCommitRestoresPreviousActiveSet() {
        var fixture = fixture(List.of(
                descriptor(
                        "dependency",
                        List.of(),
                        List.of(),
                        true,
                        DependencyModule.class
                ),
                descriptor(
                        "failing",
                        List.of(),
                        List.of(),
                        false,
                        FailingModule.class
                )
        ));
        fixture.manager.loadAsync().join();
        fixture.modules().modules.put("dependency", false);
        fixture.modules().modules.put("failing", true);

        var prepared = fixture.manager.prepareReload(fixture.configs.snapshot())
                .toCompletableFuture()
                .join();
        assertThrows(
                RuntimeException.class,
                () -> prepared.commit().toCompletableFuture().join()
        );
        prepared.rollback().toCompletableFuture().join();

        assertTrue(fixture.manager.moduleEnabled("dependency"));
        assertFalse(fixture.manager.moduleEnabled("failing"));
    }

    private enum TransactionMode {
        SUCCESS,
        PREPARE_FAILURE,
        COMMIT_FAILURE,
        ROLLBACK_FAILURE
    }

    private record Fixture(
            DefaultModuleManager manager,
            JacksonConfigRegistry configs,
            DefaultServiceRegistry services
    ) {

        private ModulesConfig modules() {
            return configs.require("modules", ModulesConfig.class);
        }

    }

    public static final class DependencyModule extends LoggingModule {

        public DependencyModule() {
            super("dependency");
        }

    }

    public static final class DependentModule extends LoggingModule {

        public DependentModule() {
            super("dependent");
        }

    }

    public static final class ProviderModule extends LoggingModule {

        public ProviderModule() {
            super("provider");
        }

    }

    public static final class ConsumerModule extends LoggingModule {

        public ConsumerModule() {
            super("consumer");
        }

    }

    public static final class RequiredDependentModule extends LoggingModule {

        public RequiredDependentModule() {
            super("required-dependent");
        }

    }

    public abstract static class LoggingModule implements CellulosesZModule {

        private final String id;

        protected LoggingModule(String id) {
            this.id = id;
        }

        @Override
        public void construct(ModuleContext context) {
            LOG.add("load:" + id);
        }

        @Override
        public void onUnload(ModuleContext context) {
            LOG.add("unload:" + id);
        }

    }

    public static final class FailingModule implements CellulosesZModule {

        private static boolean closed;

        @Override
        public void registerServices(ModuleContext context) {
            context.services().register(MarkerService.class, new MarkerService());
        }

        @Override
        public void registerEvents(ModuleContext context) {
            throw new IllegalStateException("expected load failure");
        }

    }

    public static final class TransactionModuleA extends TransactionModule {

        private static TransactionMode mode = TransactionMode.SUCCESS;
        private static CompletableFuture<Void> prepareGate;

        public TransactionModuleA() {
            super("a");
        }

        @Override
        TransactionMode mode() {
            return mode;
        }

        @Override
        CompletableFuture<Void> prepareGate() {
            return prepareGate;
        }

    }

    public static final class TransactionModuleB extends TransactionModule {

        private static TransactionMode mode = TransactionMode.SUCCESS;
        private static CompletableFuture<Void> prepareGate;

        public TransactionModuleB() {
            super("b");
        }

        @Override
        TransactionMode mode() {
            return mode;
        }

        @Override
        CompletableFuture<Void> prepareGate() {
            return prepareGate;
        }

    }

    public abstract static class TransactionModule implements CellulosesZModule {

        private final String id;

        protected TransactionModule(String id) {
            this.id = id;
        }

        @Override
        public CompletionStage<PreparedModuleReload> prepareReload(ModuleReloadContext context) {
            LOG.add("prepare:" + id);
            if (mode() == TransactionMode.PREPARE_FAILURE) {
                return CompletableFuture.failedFuture(new IllegalStateException("prepare:" + id));
            }

            var gate = prepareGate();
            var ready = gate == null
                    ? CompletableFuture.completedFuture(null)
                    : gate;
            return ready.thenApply(_ -> PreparedReloads.of(
                    () -> {
                        LOG.add("commit:" + id);
                        return mode() == TransactionMode.COMMIT_FAILURE
                                ? CompletableFuture.failedFuture(
                                new IllegalStateException("commit:" + id)
                        )
                                : CompletableFuture.completedFuture(null);
                    },
                    () -> {
                        LOG.add("rollback:" + id);
                        return mode() == TransactionMode.ROLLBACK_FAILURE
                                ? CompletableFuture.failedFuture(
                                new IllegalStateException("rollback:" + id)
                        )
                                : CompletableFuture.completedFuture(null);
                    }
            ));
        }

        abstract TransactionMode mode();

        abstract CompletableFuture<Void> prepareGate();

    }

    public static final class MarkerService implements AsyncCloseable {

        @Override
        public void stopAccepting() {
        }

        @Override
        public CompletableFuture<Void> drain() {
            FailingModule.closed = true;
            return CompletableFuture.completedFuture(null);
        }

    }

    private static final class NoopLogger implements CellulosesZLogger {

        @Override
        public void warn(String message) {
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
