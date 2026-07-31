package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AddressBookService;
import top.likoslupus.cellulosesz.api.lifecycle.AsyncInitializable;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.data.AddressBookDocument;

import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.requireNonNull;
import static top.likoslupus.cellulosesz.api.validation.Checks.requireNonEmpty;

public final class JsonAddressBookService implements
        AddressBookService,
        AsyncInitializable {

    private final StorageService storage;
    private final Path path;
    private AddressBookDocument document = new AddressBookDocument();
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonAddressBookService(
            StorageService storage,
            Path path
    ) {
        this.storage = requireNonNull(storage, "storage");
        this.path = requireNonNull(path, "path");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return storage.createIfMissing(
                        path,
                        AddressBookDocument.class,
                        AddressBookDocument::new
                )
                .thenApply(loaded -> {
                    validate(loaded);
                    return loaded;
                })
                .thenAccept(loaded -> {
                    synchronized (this) {
                        document = copy(loaded);
                    }
                });
    }

    private static void validate(AddressBookDocument candidate) {
        requireNonNull(candidate, "candidate");
        candidate.players.forEach((uuid, entry) -> {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(uuid);
            entry.name = requireNonEmpty(entry.name, "entry.name").trim();
            var address = IpAddresses.parseLiteral(entry.address)
                    .orElseThrow(() -> new IllegalStateException("Invalid stored IP address"));
            entry.address = IpAddresses.canonical(address);
        });
    }

    private static AddressBookDocument copy(AddressBookDocument source) {
        var target = new AddressBookDocument();
        source.players.forEach((uuid, existing) -> {
            var entry = new AddressBookDocument.Entry();
            entry.name = existing.name;
            entry.address = existing.address;
            target.players.put(uuid, entry);
        });
        return target;
    }

    @Override
    public synchronized CompletableFuture<Void> remember(
            UUID uuid,
            String name,
            InetAddress address
    ) {
        requireNonNull(uuid, "uuid");
        var normalizedName = requireNonEmpty(name, "name").trim();
        var normalizedAddress = IpAddresses.canonical(requireNonNull(address, "address"));
        var result = new CompletableFuture<Void>();

        mutationTail = mutationTail
                .handle((_, _) -> null)
                .thenCompose(_ -> {
                    AddressBookDocument next;
                    synchronized (this) {
                        next = copy(document);
                    }

                    var entry = new AddressBookDocument.Entry();
                    entry.name = normalizedName;
                    entry.address = normalizedAddress;
                    next.players.put(uuid.toString(), entry);

                    return storage
                            .save(path, next)
                            .handle((_, failure) -> {
                                if (failure == null) {
                                    synchronized (this) {
                                        document = next;
                                    }
                                    result.complete(null);
                                } else {
                                    result.completeExceptionally(failure);
                                }
                                return (Void) null;
                            });
                });

        mutationTail
                .whenComplete((_, failure) -> {
                    if (failure != null) {
                        result.completeExceptionally(failure);
                    }
                });
        return result;
    }

    @Override
    public synchronized Optional<InetAddress> address(UUID uuid) {
        var entry = document.players.get(
                requireNonNull(uuid, "uuid").toString()
        );
        return entry == null
                ? Optional.empty()
                : parseStored(entry.address);
    }

    @Override
    public synchronized Optional<InetAddress> address(String name) {
        var normalized = requireNonEmpty(name, "name").trim().toLowerCase(Locale.ROOT);
        return document.players.values().stream()
                .filter(entry -> entry.name.toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst().flatMap(entry -> parseStored(entry.address));
    }

    private static Optional<InetAddress> parseStored(String value) {
        var parsed = IpAddresses.parseLiteral(value);
        if (parsed.isEmpty()) {
            throw new IllegalStateException("Address book contains invalid address");
        }
        return parsed;
    }

}
