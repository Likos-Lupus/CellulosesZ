package top.likoslupus.cellulosesz.api.module;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public final class PreparedReloads {

    private PreparedReloads() {
    }

    public static PreparedModuleReload noop() {
        return of(
                () -> CompletableFuture.completedFuture(null),
                () -> CompletableFuture.completedFuture(null)
        );
    }

    public static PreparedModuleReload of(
            Supplier<? extends CompletionStage<Void>> commit,
            Supplier<? extends CompletionStage<Void>> rollback
    ) {
        return new StatefulPreparedReload(commit, rollback);
    }

    private static final class StatefulPreparedReload implements PreparedModuleReload {

        private final Supplier<? extends CompletionStage<Void>> commitAction;
        private final Supplier<? extends CompletionStage<Void>> rollbackAction;

        private State state = State.PREPARED;
        private @Nullable CompletableFuture<Void> commitFuture;
        private @Nullable CompletableFuture<Void> rollbackFuture;

        private StatefulPreparedReload(
                Supplier<? extends CompletionStage<Void>> commitAction,
                Supplier<? extends CompletionStage<Void>> rollbackAction
        ) {
            this.commitAction = requireNonNull(commitAction, "commitAction");
            this.rollbackAction = requireNonNull(rollbackAction, "rollbackAction");
        }

        @Override
        public synchronized CompletionStage<Void> commit() {
            if (commitFuture != null) {
                return commitFuture;
            }

            if (state == State.ROLLING_BACK || state == State.ROLLED_BACK) {
                return CompletableFuture.failedFuture(new IllegalStateException(
                        "Prepared reload has already been rolled back"
                ));
            }

            state = State.COMMITTING;
            commitFuture = invoke(commitAction);
            commitFuture.whenComplete((_, failure) -> {
                synchronized (StatefulPreparedReload.this) {
                    state = failure == null
                            ? State.COMMITTED
                            : State.COMMIT_FAILED;
                }
            });

            return commitFuture;
        }

        @Override
        public synchronized CompletionStage<Void> rollback() {
            if (rollbackFuture != null) {
                return rollbackFuture;
            }

            state = State.ROLLING_BACK;
            var beforeRollback = commitFuture == null
                    ? CompletableFuture.<Void>completedFuture(null)
                    : commitFuture.handle((_, _) -> (Void) null);
            rollbackFuture = beforeRollback
                    .thenCompose(_ -> invoke(rollbackAction))
                    .whenComplete((_, _) -> {
                        synchronized (StatefulPreparedReload.this) {
                            state = State.ROLLED_BACK;
                        }
                    });

            return rollbackFuture;
        }

        private static CompletableFuture<Void> invoke(
                Supplier<? extends CompletionStage<Void>> action
        ) {
            try {
                return requireNonNull(action.get(), "reload stage").toCompletableFuture();
            } catch (RuntimeException failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }

        private enum State {

            PREPARED,
            COMMITTING,
            COMMITTED,
            COMMIT_FAILED,
            ROLLING_BACK,
            ROLLED_BACK

        }

    }

}
