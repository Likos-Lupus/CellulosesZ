package top.likoslupus.cellulosesz.modules.sign.service;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncCloseable;
import top.likoslupus.cellulosesz.core.lifecycle.legacy.AsyncInitializable;
import top.likoslupus.cellulosesz.core.storage.StorageService;
import top.likoslupus.cellulosesz.modules.sign.SignConfig;
import top.likoslupus.cellulosesz.modules.sign.data.SignDocument;
import top.likoslupus.cellulosesz.modules.sign.data.StoredSign;
import top.likoslupus.cellulosesz.modules.sign.domain.*;
import top.likoslupus.cellulosesz.modules.sign.handler.CellSignHandler;

import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class DefaultSignService implements SignService, AsyncInitializable, AsyncCloseable {

    private static final Pattern LEGACY_FORMAT = Pattern.compile("(?i)[§&][0-9A-FK-OR]");
    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;
    private static final Set<String> CONFIGURED_HANDLERS = Set.of(
            "warp", "buy", "sell", "kit", "balance", "free", "trade", "enchant", "repair",
            "gamemode", "heal", "info", "mail", "randomteleport", "anvil", "cartography",
            "disposal", "grindstone", "loom", "smithing", "workbench", "spawnmob", "time", "weather"
    );
    private final PermissionService permissions;
    private final StorageService storage;
    private final Path path;
    private final Map<String, CellSignHandler> handlers = new LinkedHashMap<>();
    private final Map<CooldownKey, Long> lastUseNanos = new ConcurrentHashMap<>();
    private final Set<CooldownKey> activeUses = ConcurrentHashMap.newKeySet();
    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private volatile ConfigSnapshot config;
    private SignDocument document;
    private long revision;

    public DefaultSignService(
            SignConfig config,
            PermissionService permissions,
            StorageService storage,
            Path path
    ) {
        this.config = ConfigSnapshot.from(config);
        this.permissions = requireNonNull(permissions, "permissions");
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
        document = new SignDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .createIfMissing(
                        path,
                        SignDocument.class,
                        SignDocument::new
                )
                .thenApply(loaded -> {
                    loaded.validate();
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = loaded;
                    }
                });
    }

    public void configure(SignConfig config) {
        this.config = ConfigSnapshot.from(config);
    }

    @Override
    public synchronized void register(CellSignHandler handler) {
        var checked = requireNonNull(handler, "handler");
        var id = normalizeId(checked.id());

        if (id.isEmpty()) {
            throw new IllegalArgumentException("Sign handler id must not be blank");
        }

        if (!CONFIGURED_HANDLERS.contains(id)) {
            throw new IllegalArgumentException("Sign handler has no configuration entry: " + id);
        }

        if (handlers.putIfAbsent(id, checked) != null) {
            throw new IllegalArgumentException("Duplicate sign handler: " + id);
        }
    }

    @Override
    public synchronized List<String> handlers() {
        return List.copyOf(handlers.keySet());
    }

    @Override
    public synchronized List<String> formattedLines(List<String> lines) {
        requireNonNull(lines, "lines");

        if (lines.isEmpty()) {
            return List.copyOf(lines);
        }

        var handler = handlers.get(normalizeHeader(lines.getFirst()));
        return handler == null
                ? List.copyOf(lines)
                : normalizeLines(lines, handler.id());
    }

    @Override
    public synchronized SignMutationExecution create(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> lines
    ) {
        return replace(player, location, front, List.of(), lines, true);
    }

    @Override
    public synchronized SignMutationExecution edit(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> previousLines,
            List<String> lines
    ) {
        return replace(player, location, front, previousLines, lines, false);
    }

    @Override
    public synchronized SignMutationExecution breakSign(
            CellPlayer player,
            CellLocation location,
            List<String> frontLines,
            List<String> backLines
    ) {
        var frontKey = key(location, true);
        var backKey = key(location, false);
        var frontRecord = document.signs.get(frontKey);
        var backRecord = document.signs.get(backKey);

        if (frontRecord == null && backRecord == null) {
            return SignMutationExecution.pass();
        }

        if (frontRecord != null && !sameLines(frontRecord.lines, frontLines)
                || backRecord != null && !sameLines(backRecord.lines, backLines)
        ) {
            return completedMutation(SignUseResult.failure("service.sign.changed"));
        }

        var records = new ArrayList<StoredSign>(2);
        if (frontRecord != null) {
            records.add(frontRecord);
        }
        if (backRecord != null) {
            records.add(backRecord);
        }

        for (var record : records) {
            if (!permissions.has(
                    player,
                    permission("break", record.type)
            )) {
                return completedMutation(SignUseResult.failure(
                        "service.sign.break-no-permission",
                        MessageArguments.empty()
                ));
            }
        }

        var expectedRevision = revision;
        var result = enqueueMutation(() -> {
            if (revision != expectedRevision) {
                return MutationPlan.noSave(SignUseResult.failure("service.sign.concurrent-change"));
            }

            var next = document.copy();
            next.signs.remove(frontKey);
            next.signs.remove(backKey);

            return MutationPlan.save(
                    next,
                    SignUseResult.success(
                            "service.sign.break-success",
                            MessageArguments.empty()
                    )
            );
        });

        return SignMutationExecution.handled(result);
    }

    @Override
    public synchronized SignExecution use(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> lines,
            boolean sneaking
    ) {
        var snapshot = config;
        if (!snapshot.enabled()) {
            return SignExecution.pass();
        }

        var stored = document.signs.get(key(location, front));
        if (stored == null) {
            return SignExecution.pass();
        }

        var handler = handlers.get(stored.type);
        if (handler == null || !snapshot.enabled(stored.type)) {
            return SignExecution.pass();
        }

        if (!sameLines(stored.lines, lines)) {
            return completedUse(SignUseResult.failure("service.sign.changed"));
        }

        if (!permissions.has(
                player,
                permission("use", stored.type)
        )) {
            return completedUse(SignUseResult.failure(
                    "service.sign.no-permission",
                    MessageArguments.builder().add(handler.id()).build()
            ));
        }

        var cooldownKey = cooldownKey(player.uuid(), location, front, stored.type);

        var now = System.nanoTime();
        var cooldownNanos = snapshot.cooldownNanos();
        var previous = lastUseNanos.get(cooldownKey);
        if (previous != null && elapsedNanos(now, previous) < cooldownNanos) {
            return completedUse(SignUseResult.failure("service.sign.cooldown"));
        }

        if (!activeUses.add(cooldownKey)) {
            return completedUse(SignUseResult.failure("service.sign.cooldown"));
        }

        CompletableFuture<SignUseResult> execution;
        try {
            execution = requireNonNull(
                    handler.use(new SignUseContext(
                            player,
                            location,
                            front,
                            stored.lines,
                            sneaking
                    )),
                    "handler result"
            );
        } catch (RuntimeException exception) {
            activeUses.remove(cooldownKey);
            return completedUse(executionFailure(exception));
        }

        var result = execution.handle((value, exception) -> {
            activeUses.remove(cooldownKey);
            if (exception != null) {
                return executionFailure(unwrap(exception));
            }

            if (value.handled() && value.success()) {
                lastUseNanos.put(cooldownKey, System.nanoTime());
            }

            return value;
        });

        return SignExecution.handled(result);
    }

    private String key(CellLocation location, boolean front) {
        var c = coordinates(location);
        return location.world() + ":" + c[0] + ":" + c[1] + ":" + c[2] + ":" + (
                front
                        ? "front"
                        : "back"
        );
    }

    private boolean sameLines(List<String> stored, List<String> actual) {
        if (stored.size() != 4 || actual.size() != 4) {
            return false;
        }

        if (!normalizeHeader(stored.getFirst()).equals(normalizeHeader(actual.getFirst()))) {
            return false;
        }

        return IntStream.range(1, 4)
                .allMatch(index -> normalizeText(stored.get(index))
                        .equals(normalizeText(actual.get(index)))
                );
    }

    private SignMutationExecution completedMutation(SignUseResult result) {
        return SignMutationExecution.handled(CompletableFuture.completedFuture(
                SignMutationCommits.completed(result)
        ));
    }

    private String permission(String action, String id) {
        return "cellulosesz.sign." + action + "." + normalizeId(id);
    }

    private CompletableFuture<SignMutationCommit> enqueueMutation(Supplier<MutationPlan> supplier) {
        var prepared = new CompletableFuture<SignMutationCommit>();
        var queued = mutations.submit(() -> {
            final MutationPlan plan;

            final SignDocument previous;
            synchronized (this) {
                plan = requireNonNull(supplier.get(), "mutation plan");
                previous = document.copy();
            }

            if (!plan.save()) {
                prepared.complete(SignMutationCommits.completed(plan.result()));
                return CompletableFuture.completedFuture(null);
            }

            var operation = new CompletableFuture<Void>();
            storage
                    .save(path, plan.next())
                    .whenComplete((_, saveFailure) -> {
                        if (saveFailure != null) {
                            prepared.complete(SignMutationCommits.completed(SignUseResult.failure(
                                    "service.sign.save-failed",
                                    MessageArguments.empty()
                            )));

                            operation.complete(null);
                            return;
                        }

                        synchronized (this) {
                            document = plan.next();
                            revision++;
                        }

                        var commit = new PendingMutationCommit(plan.result());
                        prepared.complete(commit);
                        commit
                                .platformDecision()
                                .whenComplete((platformApplied, decisionFailure) -> {
                                    if (decisionFailure == null
                                            && Boolean.TRUE.equals(platformApplied)
                                    ) {
                                        commit.finish(plan.result());
                                        operation.complete(null);
                                        return;
                                    }

                                    storage
                                            .save(path, previous)
                                            .whenComplete((_, rollbackFailure) -> {
                                                if (rollbackFailure == null) {
                                                    synchronized (this) {
                                                        document = previous;
                                                        revision++;
                                                    }

                                                    commit.finish(SignUseResult.failure(
                                                            "service.sign.platform-apply-failed"
                                                    ));
                                                } else {
                                                    commit.finish(SignUseResult.failure(
                                                            "service.sign.platform-rollback-failed",
                                                            MessageArguments.empty()
                                                    ));
                                                }

                                                operation.complete(null);
                                            });
                                });
                    });

            return operation;
        });

        queued.whenComplete((_, failure) -> {
            if (failure != null) {
                prepared.completeExceptionally(unwrap(failure));
            }
        });

        return prepared;
    }

    private int[] coordinates(CellLocation location) {
        return new int[]{
                floor(location.x()),
                floor(location.y()),
                floor(location.z())
        };
    }

    private Throwable unwrap(Throwable throwable) {
        var current = throwable;
        while (
                (
                        current instanceof CompletionException
                                || current instanceof ExecutionException
                )
                        && current.getCause() != null
        ) {
            current = current.getCause();
        }
        return current;
    }

    private int floor(double value) {
        if (!Double.isFinite(value)
                || value < Integer.MIN_VALUE
                || value > Integer.MAX_VALUE
        ) {
            throw new IllegalArgumentException(
                    "Sign coordinate is outside the supported block range"
            );
        }

        return (int) Math.floor(value);
    }

    private String normalizeHeader(String value) {
        var normalized = normalizeText(value);
        if (normalized.startsWith("[")
                && normalized.endsWith("]")
                && normalized.length() > 2
        ) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalizeId(normalized);
    }

    private List<String> normalizeLines(List<String> lines, String displayId) {
        var result = new ArrayList<>(List.of("", "", "", ""));
        IntStream.range(0, Math.min(4, lines.size()))
                .forEach(index ->
                        result.set(index, normalizeText(lines.get(index)))
                );
        result.set(0, "§1[" + displayId + "]");
        return List.copyOf(result);
    }

    private String normalizeText(String value) {
        var normalized = Normalizer.normalize(
                requireNonNull(value, "value"),
                Normalizer.Form.NFKC
        );
        return LEGACY_FORMAT.matcher(normalized)
                .replaceAll("")
                .strip();
    }

    private String normalizeId(String value) {
        return requireNonNull(value, "value").strip().toLowerCase(Locale.ROOT);
    }

    private String safeReason(Throwable exception) {
        var message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }

    private SignMutationExecution replace(
            CellPlayer player,
            CellLocation location,
            boolean front,
            List<String> previousLines,
            List<String> lines,
            boolean creating
    ) {
        requireNonNull(previousLines, "previousLines");
        requireNonNull(lines, "lines");

        var snapshot = config;
        if (!snapshot.enabled() || lines.isEmpty()) {
            return SignMutationExecution.pass();
        }

        var storageKey = key(location, front);
        var existing = document.signs.get(storageKey);

        var id = normalizeHeader(lines.getFirst());
        var handler = handlers.get(id);
        if (handler == null || !snapshot.enabled(id)) {
            if (existing == null) {
                return SignMutationExecution.pass();
            }

            if (!sameLines(existing.lines, previousLines)) {
                return completedMutation(SignUseResult.failure("service.sign.changed"));
            }

            if (!permissions.has(player, permission("edit", existing.type))) {
                return completedMutation(SignUseResult.failure(
                        "service.sign.edit-no-permission",
                        MessageArguments.empty()
                ));
            }

            var expectedRevision = revision;
            return SignMutationExecution.handled(enqueueMutation(() -> {
                if (revision != expectedRevision) {
                    return MutationPlan.noSave(SignUseResult.failure(
                            "service.sign.concurrent-change"
                    ));
                }

                var next = document.copy();
                next.signs.remove(storageKey);

                return MutationPlan.save(
                        next,
                        SignUseResult.success("service.sign.removed")
                );
            }));
        }

        var action = creating || existing == null
                ? "create"
                : "edit";
        if (!permissions.has(player, permission(action, id))) {
            return completedMutation(SignUseResult.failure(
                    action.equals("create")
                            ? "service.sign.create-no-permission"
                            : "service.sign.edit-no-permission",
                    MessageArguments.empty()
            ));
        }

        if (existing != null) {
            if (!sameLines(existing.lines, previousLines)) {
                return completedMutation(SignUseResult.failure("service.sign.changed"));
            }

            if (!existing.type.equals(id) && !permissions.has(
                    player,
                    permission("edit", existing.type)
            )) {
                return completedMutation(SignUseResult.failure(
                        "service.sign.edit-no-permission",
                        MessageArguments.empty()
                ));
            }
        }

        var normalized = normalizeLines(lines, handler.id());
        var context = new SignUseContext(
                player,
                location,
                front,
                normalized,
                false
        );
        final SignUseResult validation;
        try {
            validation = requireNonNull(handler.validate(context), "validation result");
        } catch (RuntimeException exception) {
            return completedMutation(executionFailure(exception));
        }

        if (!validation.success()) {
            return completedMutation(validation);
        }

        var coordinates = coordinates(location);
        var record = new StoredSign(
                location.world(),
                coordinates[0], coordinates[1], coordinates[2],
                front,
                id,
                normalized,
                player.uuid()
        );
        var expectedRevision = revision;
        var success = SignUseResult.success(
                creating
                        ? "service.sign.create-success"
                        : "service.sign.edit-success",
                MessageArguments.empty()
        );

        return SignMutationExecution.handled(enqueueMutation(() -> {
            if (revision != expectedRevision) {
                return MutationPlan.noSave(SignUseResult.failure("service.sign.concurrent-change"));
            }

            var next = document.copy();
            next.signs.put(storageKey, record);
            return MutationPlan.save(next, success);
        }));
    }

    private SignExecution completedUse(SignUseResult result) {
        return SignExecution.handled(CompletableFuture.completedFuture(result));
    }

    private boolean enabled(String id) {
        return config.enabled(id);
    }

    private CooldownKey cooldownKey(
            UUID player,
            CellLocation location,
            boolean front,
            String type
    ) {
        var c = coordinates(location);
        return new CooldownKey(
                player,
                location.world(),
                c[0],
                c[1],
                c[2],
                front,
                normalizeId(type)
        );
    }

    private long elapsedNanos(long now, long previous) {
        var elapsed = now - previous;
        return elapsed < 0L
                ? Long.MAX_VALUE
                : elapsed;
    }

    private SignUseResult executionFailure(Throwable exception) {
        return SignUseResult.failure(
                "service.sign.execution-failed",
                MessageArguments.builder().add(safeReason(exception)).build()
        );
    }

    @Override
    public void stopAccepting() {
        mutations.stopAccepting();
    }

    @Override
    public CompletableFuture<Void> drain() {
        return mutations.drain();
    }

    private static final class PendingMutationCommit implements SignMutationCommit {

        private final SignUseResult result;
        private final CompletableFuture<Boolean> platformDecision = new CompletableFuture<>();
        private final CompletableFuture<SignUseResult> completion = new CompletableFuture<>();

        private PendingMutationCommit(SignUseResult result) {
            this.result = requireNonNull(result, "result");
        }

        @Override
        public SignUseResult result() {
            return result;
        }

        @Override
        public boolean platformActionRequired() {
            return true;
        }

        @Override
        public CompletableFuture<SignUseResult> complete(boolean platformApplied) {
            platformDecision.complete(platformApplied);
            return completion;
        }

        private CompletableFuture<Boolean> platformDecision() {
            return platformDecision;
        }

        private void finish(SignUseResult finalResult) {
            completion.complete(requireNonNull(finalResult, "finalResult"));
        }

    }

    private record MutationPlan(
            SignDocument next,
            SignUseResult result,
            boolean save
    ) {

        private MutationPlan {
            requireNonNull(next, "next");
            requireNonNull(result, "result");
        }

        static MutationPlan save(SignDocument next, SignUseResult result) {
            return new MutationPlan(next, result, true);
        }

        static MutationPlan noSave(SignUseResult result) {
            return new MutationPlan(new SignDocument(), result, false);
        }

    }

    private record CooldownKey(
            UUID player,
            String world,
            int x,
            int y,
            int z,
            boolean front,
            String type
    ) {

        private CooldownKey {
            requireNonNull(player, "player");
            requireNonNull(world, "world");
            requireNonNull(type, "type");
        }

    }

    private record ConfigSnapshot(
            boolean enabled,
            long cooldownNanos,
            Set<String> enabledHandlers
    ) {

        private ConfigSnapshot {
            if (cooldownNanos < 0L) {
                throw new IllegalArgumentException("cooldownNanos must not be negative");
            }

            enabledHandlers = Set.copyOf(requireNonNull(enabledHandlers, "enabledHandlers"));
        }

        static ConfigSnapshot from(SignConfig source) {
            var config = requireNonNull(source, "config");
            requireNonNull(config.interaction, "config.interaction");
            requireNonNull(config.signs, "config.signs");

            if (config.interaction.cooldownTicks < 0) {
                throw new IllegalArgumentException("interaction.cooldownTicks must not be negative");
            }

            var enabled = new LinkedHashSet<String>();
            CONFIGURED_HANDLERS.stream()
                    .filter(config.signs::enabled)
                    .forEach(enabled::add);
            return new ConfigSnapshot(
                    config.enabled,
                    Math.multiplyExact(config.interaction.cooldownTicks, 50_000_000L),
                    enabled
            );
        }

        boolean enabled(String id) {
            return enabled && enabledHandlers.contains(id.toLowerCase(Locale.ROOT));
        }

    }

}
