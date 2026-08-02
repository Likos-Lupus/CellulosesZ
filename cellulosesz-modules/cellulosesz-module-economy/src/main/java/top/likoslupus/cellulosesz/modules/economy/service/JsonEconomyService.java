package top.likoslupus.cellulosesz.modules.economy.service;

import top.likoslupus.cellulosesz.api.economy.*;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncCloseable;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.module.PreparedModuleReload;
import top.likoslupus.cellulosesz.api.module.PreparedReloads;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.core.concurrent.SerialAsyncQueue;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;
import top.likoslupus.cellulosesz.modules.economy.data.EconomyDocument;
import top.likoslupus.cellulosesz.modules.economy.data.TransactionLogEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

import static top.likoslupus.cellulosesz.api.validation.Checks.requireInRange;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonNegative;

public final class JsonEconomyService
        implements EconomyService, AsyncInitializable, AsyncCloseable {

    private static final int MAX_LOG_ENTRIES = 500;
    private static final int MAXIMUM_PENDING_MUTATIONS = 4_096;

    private final StorageService storage;
    private final Path path;
    private final CellulosesZLogger logger;
    private final SerialAsyncQueue mutations = new SerialAsyncQueue(
            Runnable::run,
            MAXIMUM_PENDING_MUTATIONS
    );
    private EconomyDocument document;
    private volatile ConfigSnapshot config;
    private volatile List<BalanceEntry> cachedTop = List.of();
    private volatile long cachedTopAt;

    public JsonEconomyService(
            StorageService storage,
            EconomyConfig config,
            Path directory,
            CellulosesZLogger logger
    ) {
        this.storage = storage;
        this.config = ConfigSnapshot.from(config);
        this.path = directory.resolve("economy.json");
        this.logger = logger;
        this.document = new EconomyDocument();
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage
                .createIfMissing(
                        path,
                        EconomyDocument.class,
                        EconomyDocument::new
                )
                .thenApply(loaded -> {
                    validateDocument(loaded, config);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = loaded;
                    }
                });
    }

    private void validateDocument(
            EconomyDocument candidate,
            ConfigSnapshot snapshot
    ) {
        candidate.balances.forEach((uuid, amount) -> {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(uuid);
            if (!withinBounds(money(amount, snapshot), snapshot)) {
                throw new IllegalStateException("Stored balance is outside configured bounds");
            }
        });
    }

    private boolean withinBounds(BigDecimal amount, ConfigSnapshot snapshot) {
        return amount.compareTo(snapshot.minimum()) >= 0
                && amount.compareTo(snapshot.maximum()) <= 0;
    }

    private BigDecimal money(String value, ConfigSnapshot snapshot) {
        return normalizeAmount(new BigDecimal(value), snapshot);
    }

    private BigDecimal normalizeAmount(BigDecimal amount, ConfigSnapshot snapshot) {
        return amount.setScale(snapshot.scale(), RoundingMode.HALF_UP);
    }

    public synchronized void validateConfiguration(EconomyConfig candidate) {
        validateDocument(document, ConfigSnapshot.from(candidate));
    }

    public synchronized PreparedModuleReload prepareConfiguration(EconomyConfig candidate) {
        var replacement = ConfigSnapshot.from(candidate);
        validateDocument(document, replacement);

        var previousConfig = config;
        var previousTop = cachedTop;
        var previousTopAt = cachedTopAt;
        return PreparedReloads.of(
                () -> {
                    synchronized (JsonEconomyService.this) {
                        validateDocument(document, replacement);
                        config = replacement;
                        invalidateTop();
                    }

                    return CompletableFuture.completedFuture(null);
                },
                () -> {
                    synchronized (JsonEconomyService.this) {
                        config = previousConfig;
                        cachedTop = previousTop;
                        cachedTopAt = previousTopAt;
                    }

                    return CompletableFuture.completedFuture(null);
                }
        );
    }

    private void invalidateTop() {
        cachedTop = List.of();
        cachedTopAt = 0L;
    }

    @Override
    public synchronized BigDecimal balance(UUID uuid) {
        return read(document, uuid, config);
    }

    @Override
    public String format(BigDecimal amount) {
        var snapshot = config;
        var symbols = DecimalFormatSymbols.getInstance(Locale.ROOT);

        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');

        var pattern = snapshot.grouping()
                ? "#,##0"
                : "0";
        if (snapshot.scale() > 0) {
            pattern += "." + "0".repeat(snapshot.scale());
        }

        var normalized = normalizeAmount(amount, snapshot);
        var number = new DecimalFormat(pattern, symbols).format(normalized);
        var spacing = snapshot.spaceBetweenSymbolAndAmount()
                ? " "
                : "";

        var money = snapshot.symbolBefore()
                ? snapshot.symbol() + spacing + number
                : number + spacing + snapshot.symbol();
        var unit = normalized.abs().compareTo(BigDecimal.ONE.setScale(snapshot.scale())) == 0
                ? snapshot.singular()
                : snapshot.plural();

        return snapshot.showName()
                ? money + " " + unit
                : money;
    }

    @Override
    public CompletableFuture<TransactionResult> deposit(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    ) {
        return enqueue(current -> {
            var snapshot = config;
            var normalized = normalizeAmount(amount, snapshot);
            var balance = read(current, uuid, snapshot);

            if (normalized.signum() <= 0) {
                return failureOutcome(
                        current,
                        null,
                        uuid,
                        normalized,
                        cause,
                        "service.economy.amount-positive",
                        balance,
                        snapshot
                );
            }

            var nextBalance = balance.add(normalized);
            if (nextBalance.compareTo(snapshot.maximum()) > 0) {
                return failureOutcome(
                        current,
                        null,
                        uuid,
                        normalized,
                        cause,
                        "service.economy.balance-maximum",
                        balance,
                        snapshot
                );
            }

            var next = copy(current);
            write(next, uuid, nextBalance, snapshot);
            append(
                    next,
                    logEntry(
                            null,
                            uuid,
                            normalized,
                            cause,
                            true,
                            "service.economy.deposit-success",
                            snapshot
                    )
            );

            return MutationOutcome.balanceChange(
                    next,
                    TransactionResult.success(
                            "service.economy.deposit-success",
                            normalized,
                            nextBalance
                    )
            );
        });
    }

    @Override
    public CompletableFuture<TransactionResult> withdraw(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    ) {
        return enqueue(current -> {
            var snapshot = config;
            var normalized = normalizeAmount(amount, snapshot);
            var balance = read(current, uuid, snapshot);

            if (normalized.signum() <= 0) {
                return failureOutcome(
                        current,
                        uuid,
                        null,
                        normalized,
                        cause,
                        "service.economy.amount-positive",
                        balance,
                        snapshot
                );
            }

            var nextBalance = balance.subtract(normalized);
            if (nextBalance.compareTo(snapshot.minimum()) < 0) {
                return failureOutcome(
                        current,
                        uuid,
                        null,
                        normalized,
                        cause,
                        "service.economy.insufficient-funds",
                        balance,
                        snapshot
                );
            }

            var next = copy(current);
            write(next, uuid, nextBalance, snapshot);
            append(
                    next,
                    logEntry(
                            uuid,
                            null,
                            normalized,
                            cause,
                            true,
                            "service.economy.withdraw-success",
                            snapshot
                    )
            );

            return MutationOutcome.balanceChange(
                    next,
                    TransactionResult.success(
                            "service.economy.withdraw-success",
                            normalized,
                            nextBalance
                    )
            );
        });
    }

    @Override
    public CompletableFuture<TransactionResult> setBalance(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    ) {
        return enqueue(current -> {
            var snapshot = config;
            var normalized = normalizeAmount(amount, snapshot);
            var balance = read(current, uuid, snapshot);

            if (!withinBounds(normalized, snapshot)) {
                return failureOutcome(
                        current,
                        null,
                        uuid,
                        normalized,
                        cause,
                        "service.economy.balance-out-of-range",
                        balance,
                        snapshot
                );
            }

            var next = copy(current);
            write(next, uuid, normalized, snapshot);
            append(
                    next,
                    logEntry(
                            null,
                            uuid,
                            normalized,
                            cause,
                            true,
                            "service.economy.balance-set",
                            snapshot
                    )
            );

            return MutationOutcome.balanceChange(
                    next,
                    TransactionResult.success(
                            "service.economy.balance-set",
                            normalized,
                            normalized
                    )
            );
        });
    }

    @Override
    public CompletableFuture<TransactionResult> transfer(
            UUID from,
            UUID to,
            BigDecimal amount,
            TransactionCause cause
    ) {
        return transferMany(from, List.of(to), amount, cause);
    }

    @Override
    public CompletableFuture<TransactionResult> transferMany(
            UUID from,
            Collection<UUID> recipients,
            BigDecimal amountEach,
            TransactionCause cause
    ) {
        var immutableRecipients = List.copyOf(recipients);
        return enqueue(current -> transferOutcome(
                current,
                from,
                immutableRecipients,
                amountEach,
                cause
        ));
    }

    @Override
    public synchronized List<BalanceEntry> topBalances(
            int limit,
            BalanceFilter filter
    ) {
        var minimum = filter.minimum();
        var maximum = filter.maximum();

        if (limit <= 0) {
            return List.of();
        }

        var snapshot = config;
        var now = System.currentTimeMillis();

        long ttl;
        try {
            ttl = Math.multiplyExact(snapshot.balanceTopCacheSeconds(), 1000L);
        } catch (ArithmeticException exception) {
            ttl = Long.MAX_VALUE;
        }

        if (cachedTop.isEmpty() || now - cachedTopAt > ttl) {
            cachedTop = document.balances.entrySet().stream()
                    .map(entry -> new BalanceEntry(
                            UUID.fromString(entry.getKey()),
                            money(entry.getValue(), snapshot)
                    ))
                    .sorted(Comparator.comparing(BalanceEntry::balance)
                            .reversed()
                            .thenComparing(entry -> entry.uuid().toString())
                    )
                    .toList();
            cachedTopAt = now;
        }

        return cachedTop.stream()
                .filter(entry -> minimum.isEmpty()
                        || entry.balance().compareTo(minimum.orElseThrow()) >= 0
                )
                .filter(entry -> maximum.isEmpty()
                        || entry.balance().compareTo(maximum.orElseThrow()) <= 0
                )
                .limit(limit)
                .toList();
    }

    private BigDecimal read(
            EconomyDocument source,
            UUID uuid,
            ConfigSnapshot snapshot
    ) {
        var value = source.balances.get(uuid.toString());
        return value == null
                ? snapshot.starting()
                : money(value, snapshot);
    }

    private MutationOutcome transferOutcome(
            EconomyDocument current,
            UUID from,
            Collection<UUID> recipients,
            BigDecimal amountEach,
            TransactionCause cause
    ) {
        var snapshot = config;
        var uniqueRecipients = new LinkedHashSet<>(recipients);
        uniqueRecipients.remove(from);

        var normalized = normalizeAmount(amountEach, snapshot);
        var fromBalance = read(current, from, snapshot);

        if (uniqueRecipients.isEmpty()) {
            return failureOutcome(
                    current,
                    from,
                    null,
                    normalized,
                    cause,
                    "service.economy.self-payment",
                    fromBalance,
                    snapshot
            );
        }

        if (normalized.signum() <= 0) {
            return failureOutcome(
                    current,
                    from,
                    null,
                    normalized,
                    cause,
                    "service.economy.amount-positive",
                    fromBalance,
                    snapshot
            );
        }

        var total = normalized.multiply(BigDecimal.valueOf(uniqueRecipients.size()));
        var nextFrom = fromBalance.subtract(total);

        if (nextFrom.compareTo(snapshot.minimum()) < 0) {
            return failureOutcome(
                    current,
                    from,
                    null,
                    total,
                    cause,
                    "service.economy.insufficient-funds",
                    fromBalance,
                    snapshot
            );
        }

        var changes = new LinkedHashMap<UUID, BigDecimal>();
        changes.put(from, nextFrom);

        for (var recipient : uniqueRecipients) {
            var recipientBalance = read(current, recipient, snapshot);
            var nextRecipient = recipientBalance.add(normalized);

            if (nextRecipient.compareTo(snapshot.maximum()) > 0) {
                return failureOutcome(
                        current,
                        from,
                        recipient,
                        normalized,
                        cause,
                        "service.economy.recipient-maximum",
                        fromBalance,
                        snapshot
                );
            }
            changes.put(recipient, nextRecipient);
        }

        var next = copy(current);
        changes.forEach((uuid, value) -> write(next, uuid, value, snapshot));
        uniqueRecipients.forEach(recipient -> append(
                next,
                logEntry(
                        from,
                        recipient,
                        normalized,
                        cause,
                        true,
                        "service.economy.transfer-success",
                        snapshot
                )
        ));

        return MutationOutcome.balanceChange(
                next,
                TransactionResult.success(
                        "service.economy.transfer-success",
                        total,
                        nextFrom
                )
        );
    }

    private CompletableFuture<TransactionResult> enqueue(
            Function<EconomyDocument, MutationOutcome> operation
    ) {
        return mutations.submit(() -> {
            EconomyDocument current;
            synchronized (this) {
                current = copy(document);
            }

            var outcome = operation.apply(current);
            return storage
                    .save(path, outcome.document())
                    .handle((_, saveFailure) -> {
                        if (saveFailure == null) {
                            synchronized (this) {
                                document = outcome.document();
                                if (outcome.balanceChanged()) {
                                    invalidateTop();
                                }
                            }

                            return outcome.result();
                        }

                        if (outcome.balanceChanged()) {
                            logger.error(
                                    "Failed to persist the atomic economy document",
                                    saveFailure
                            );

                            return TransactionResult.failure(
                                    "service.economy.persistence-failed",
                                    outcome.result().amount(),
                                    outcome.result().balance()
                            );
                        }

                        logger.error(
                                "Failed to persist an economy failure audit entry",
                                saveFailure
                        );

                        return outcome.result();
                    });
        });
    }

    private MutationOutcome failureOutcome(
            EconomyDocument current,
            @Nullable UUID from,
            @Nullable UUID to,
            BigDecimal amount,
            TransactionCause cause,
            String message,
            BigDecimal balance,
            ConfigSnapshot snapshot
    ) {
        var next = copy(current);
        append(
                next,
                logEntry(
                        from,
                        to,
                        amount,
                        cause,
                        false,
                        message,
                        snapshot
                )
        );

        return MutationOutcome.auditOnly(
                next,
                TransactionResult.failure(message, amount, balance)
        );
    }

    private EconomyDocument copy(EconomyDocument source) {
        var copy = new EconomyDocument();
        copy.balances = new LinkedHashMap<>(source.balances);
        copy.transactions = source.transactions.stream()
                .map(this::copy)
                .collect(Collectors.toCollection(ArrayList::new));
        return copy;
    }

    private TransactionLogEntry copy(TransactionLogEntry source) {
        var copy = new TransactionLogEntry();
        copy.at = source.at;
        copy.causeType = source.causeType;
        copy.actor = source.actor;
        copy.note = source.note;
        copy.amount = source.amount;
        copy.from = source.from;
        copy.to = source.to;
        copy.success = source.success;
        copy.message = source.message;
        return copy;
    }

    private void append(EconomyDocument target, TransactionLogEntry entry) {
        target.transactions.add(entry);
        while (target.transactions.size() > MAX_LOG_ENTRIES) {
            target.transactions.removeFirst();
        }
    }

    private TransactionLogEntry logEntry(
            @Nullable UUID from,
            @Nullable UUID to,
            BigDecimal amount,
            TransactionCause cause,
            boolean success,
            String message,
            ConfigSnapshot snapshot
    ) {
        var entry = new TransactionLogEntry();
        entry.from = from == null
                ? null
                : from.toString();
        entry.to = to == null
                ? null
                : to.toString();
        entry.amount = normalizeAmount(amount, snapshot).toPlainString();
        entry.causeType = cause.type();
        entry.actor = cause.actor();
        entry.note = cause.note();
        entry.success = success;
        entry.message = message;
        return entry;
    }

    private void write(
            EconomyDocument target,
            UUID uuid,
            BigDecimal amount,
            ConfigSnapshot snapshot
    ) {
        target.balances.put(
                uuid.toString(),
                normalizeAmount(amount, snapshot).toPlainString()
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

    private record MutationOutcome(
            EconomyDocument document,
            TransactionResult result,
            boolean balanceChanged
    ) {

        private static MutationOutcome balanceChange(
                EconomyDocument document,
                TransactionResult result
        ) {
            return new MutationOutcome(document, result, true);
        }

        private static MutationOutcome auditOnly(
                EconomyDocument document,
                TransactionResult result
        ) {
            return new MutationOutcome(document, result, false);
        }

    }

    private record ConfigSnapshot(
            String singular,
            String plural,
            String symbol,
            boolean symbolBefore,
            boolean spaceBetweenSymbolAndAmount,
            boolean grouping,
            boolean showName,
            int scale,
            BigDecimal starting,
            BigDecimal minimum,
            BigDecimal maximum,
            long balanceTopCacheSeconds
    ) {

        private static ConfigSnapshot from(EconomyConfig source) {
            var scale = requireInRange(
                    source.currency.scale,
                    0,
                    8,
                    "currency.scale"
            );
            var starting = new BigDecimal(source.startingBalance).setScale(
                    scale,
                    RoundingMode.HALF_UP
            );
            var minimum = new BigDecimal(source.minimumBalance).setScale(
                    scale,
                    RoundingMode.HALF_UP
            );
            var maximum = new BigDecimal(source.maximumBalance).setScale(
                    scale,
                    RoundingMode.HALF_UP
            );

            if (minimum.compareTo(maximum) > 0
                    || starting.compareTo(minimum) < 0
                    || starting.compareTo(maximum) > 0
            ) {
                throw new IllegalArgumentException("Economy balance bounds are inconsistent");
            }

            return new ConfigSnapshot(
                    source.currency.singular,
                    source.currency.plural,
                    source.currency.symbol,
                    source.currency.symbolBefore,
                    source.currency.spaceBetweenSymbolAndAmount,
                    source.currency.grouping,
                    source.currency.showName,
                    scale,
                    starting,
                    minimum,
                    maximum,
                    requireNonNegative(source.balanceTop.cacheSeconds, "balanceTop.cacheSeconds")
            );
        }

    }

}
