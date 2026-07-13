package top.likoslupus.cellulosesz.modules.economy.service;

import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.economy.BalanceEntry;
import top.likoslupus.cellulosesz.api.economy.EconomyService;
import top.likoslupus.cellulosesz.api.economy.TransactionCause;
import top.likoslupus.cellulosesz.api.economy.TransactionResult;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.economy.EconomyConfig;
import top.likoslupus.cellulosesz.modules.economy.data.EconomyDocument;
import top.likoslupus.cellulosesz.modules.economy.data.TransactionLogDocument;
import top.likoslupus.cellulosesz.modules.economy.data.TransactionLogEntry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.*;

public final class JsonEconomyService implements EconomyService {

    private static final int MAX_LOG_ENTRIES = 500;

    private final StorageService storage;
    private final EconomyConfig config;
    private final Path accountsPath;
    private final Path logPath;
    private final CellulosesZLogger logger;
    private final EconomyDocument document;
    private final TransactionLogDocument log;

    public JsonEconomyService(
            StorageService storage,
            EconomyConfig config,
            Path directory,
            CellulosesZLogger logger
    ) {
        this.storage = storage;
        this.config = config;
        this.accountsPath = directory.resolve("accounts.json");
        this.logPath = directory.resolve("transactions.json");
        this.logger = logger;
        this.document = storage.load(accountsPath, EconomyDocument.class, EconomyDocument::new).join();
        this.log = storage.load(logPath, TransactionLogDocument.class, TransactionLogDocument::new).join();
    }

    @Override
    public synchronized BigDecimal balance(UUID uuid) {
        return read(uuid);
    }

    @Override
    public synchronized TransactionResult deposit(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    ) {
        var normalized = normalizeAmount(amount);
        if (normalized.signum() < 0) {
            return record(
                    null,
                    uuid,
                    normalized,
                    cause,
                    false,
                    "service.economy.negative-amount",
                    read(uuid)
            );
        }

        var snapshot = snapshotBalances();
        var current = read(uuid);
        var next = current.add(normalized);
        var maximum = money(config.maximumBalance);
        if (next.compareTo(maximum) > 0) {
            return record(
                    null,
                    uuid,
                    normalized,
                    cause,
                    false,
                    "service.economy.balance-maximum",
                    current
            );
        }

        write(uuid, next);
        if (!persistAccounts(snapshot)) {
            return record(
                    null,
                    uuid,
                    normalized,
                    cause,
                    false,
                    "service.economy.persistence-failed",
                    current
            );
        }
        return record(
                null,
                uuid,
                normalized,
                cause,
                true,
                "service.economy.deposit-success",
                next
        );
    }

    @Override
    public synchronized TransactionResult withdraw(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    ) {
        var normalized = normalizeAmount(amount);
        if (normalized.signum() < 0) {
            return record(
                    uuid,
                    null,
                    normalized,
                    cause,
                    false,
                    "service.economy.negative-amount",
                    read(uuid)
            );
        }

        var snapshot = snapshotBalances();
        var current = read(uuid);
        var next = current.subtract(normalized);
        var minimum = money(config.minimumBalance);
        if (next.compareTo(minimum) < 0) {
            return record(
                    uuid,
                    null,
                    normalized,
                    cause,
                    false,
                    "service.economy.insufficient-funds",
                    current
            );
        }

        write(uuid, next);
        if (!persistAccounts(snapshot)) {
            return record(
                    uuid,
                    null,
                    normalized,
                    cause,
                    false,
                    "service.economy.persistence-failed",
                    current
            );
        }
        return record(
                uuid,
                null,
                normalized,
                cause,
                true,
                "service.economy.withdraw-success",
                next
        );
    }

    @Override
    public synchronized TransactionResult setBalance(
            UUID uuid,
            BigDecimal amount,
            TransactionCause cause
    ) {
        var normalized = normalizeAmount(amount);
        var minimum = money(config.minimumBalance);
        var maximum = money(config.maximumBalance);
        if (normalized.compareTo(minimum) < 0 || normalized.compareTo(maximum) > 0) {
            return record(
                    null,
                    uuid,
                    normalized,
                    cause,
                    false,
                    "service.economy.balance-out-of-range",
                    read(uuid)
            );
        }

        var snapshot = snapshotBalances();
        var current = read(uuid);
        write(uuid, normalized);
        if (!persistAccounts(snapshot)) {
            return record(
                    null,
                    uuid,
                    normalized,
                    cause,
                    false,
                    "service.economy.persistence-failed",
                    current
            );
        }
        return record(
                null,
                uuid,
                normalized,
                cause,
                true,
                "service.economy.balance-set",
                normalized
        );
    }

    @Override
    public synchronized TransactionResult transfer(
            UUID from,
            UUID to,
            BigDecimal amount,
            TransactionCause cause
    ) {
        return transferMany(from, List.of(to), amount, cause);
    }

    @Override
    public synchronized TransactionResult transferMany(
            UUID from,
            Collection<UUID> recipients,
            BigDecimal amountEach,
            TransactionCause cause
    ) {
        var snapshot = snapshotBalances();
        var uniqueRecipients = new LinkedHashSet<>(recipients);
        uniqueRecipients.remove(from);
        var normalized = normalizeAmount(amountEach);

        if (uniqueRecipients.isEmpty()) {
            return TransactionResult.failure(
                    "service.economy.self-payment",
                    normalized,
                    read(from)
            );
        }
        if (normalized.signum() <= 0) {
            return TransactionResult.failure(
                    "service.economy.amount-positive",
                    normalized,
                    read(from)
            );
        }

        var total = normalized.multiply(BigDecimal.valueOf(uniqueRecipients.size()));
        var fromBalance = read(from);
        var nextFrom = fromBalance.subtract(total);
        if (nextFrom.compareTo(money(config.minimumBalance)) < 0) {
            return record(
                    from,
                    null,
                    total,
                    cause,
                    false,
                    "service.economy.insufficient-funds",
                    fromBalance
            );
        }

        var nextRecipients = new LinkedHashMap<UUID, BigDecimal>();
        var maximum = money(config.maximumBalance);
        for (var recipient : uniqueRecipients) {
            var next = read(recipient).add(normalized);
            if (next.compareTo(maximum) > 0) {
                return record(
                        from,
                        recipient,
                        normalized,
                        cause,
                        false,
                        "service.economy.recipient-maximum",
                        fromBalance
                );
            }
            nextRecipients.put(recipient, next);
        }

        write(from, nextFrom);
        nextRecipients.forEach(this::write);

        if (!persistAccounts(snapshot)) {
            return record(
                    from,
                    null,
                    total,
                    cause,
                    false,
                    "service.economy.persistence-failed",
                    fromBalance
            );
        }

        uniqueRecipients.forEach(recipient ->
                record(
                        from,
                        recipient,
                        normalized,
                        cause,
                        true,
                        "service.economy.transfer-success",
                        nextFrom
                )
        );
        return TransactionResult.success("service.economy.transfer-success", total, nextFrom);
    }

    @Override
    public synchronized List<BalanceEntry> topBalances(int limit) {
        return document.balances.entrySet().stream()
                .map(entry -> new BalanceEntry(
                        UUID.fromString(entry.getKey()),
                        money(entry.getValue())
                ))
                .sorted(Comparator.comparing(BalanceEntry::balance).reversed())
                .limit(Math.max(1, limit))
                .toList();
    }

    private TransactionResult record(
            @Nullable UUID from,
            @Nullable UUID to,
            BigDecimal amount,
            TransactionCause cause,
            boolean success,
            String message,
            BigDecimal resultingBalance
    ) {
        var entry = new TransactionLogEntry();
        entry.from = from == null ? null : from.toString();
        entry.to = to == null ? null : to.toString();
        entry.amount = normalizeAmount(amount).toPlainString();
        entry.causeType = cause.type();
        entry.actor = cause.actor();
        entry.note = cause.note();
        entry.success = success;
        entry.message = message;
        log.entries.add(entry);

        while (log.entries.size() > MAX_LOG_ENTRIES) log.entries.removeFirst();
        var snapshot = new TransactionLogDocument();
        snapshot.entries.addAll(log.entries);
        storage.save(logPath, snapshot)
                .whenComplete((_, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to save economy transaction log", exception);
                    }
                });
        return success
                ? TransactionResult.success(message, normalizeAmount(amount), resultingBalance)
                : TransactionResult.failure(message, normalizeAmount(amount), resultingBalance);
    }

    private Map<String, String> snapshotBalances() {
        return new LinkedHashMap<>(document.balances);
    }

    private void write(UUID uuid, BigDecimal amount) {
        document.balances.put(uuid.toString(), normalizeAmount(amount).toPlainString());
    }

    private boolean persistAccounts(Map<String, String> snapshot) {
        try {
            storage.save(accountsPath, document).join();
            return true;
        } catch (RuntimeException exception) {
            document.balances.clear();
            document.balances.putAll(snapshot);
            logger.error("Failed to persist economy accounts; in-memory balances were rolled back", exception);
            return false;
        }
    }

    private BigDecimal read(UUID uuid) {
        var value = document.balances.computeIfAbsent(
                uuid.toString(),
                _ -> money(config.startingBalance).toPlainString()
        );
        return money(value);
    }

    private BigDecimal money(String value) {
        try {
            return normalizeAmount(new BigDecimal(value));
        } catch (RuntimeException _) {
            return BigDecimal.ZERO.setScale(config.currency.scale, RoundingMode.HALF_UP);
        }
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return amount.setScale(config.currency.scale, RoundingMode.HALF_UP);
    }

    public String format(BigDecimal amount) {
        return config.currency.symbol + normalizeAmount(amount).toPlainString();
    }

}
