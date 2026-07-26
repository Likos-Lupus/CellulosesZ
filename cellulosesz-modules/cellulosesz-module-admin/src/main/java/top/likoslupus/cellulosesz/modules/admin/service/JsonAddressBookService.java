package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AddressBookService;
import top.likoslupus.cellulosesz.api.storage.StorageService;
import top.likoslupus.cellulosesz.modules.admin.data.AddressBookDocument;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class JsonAddressBookService implements AddressBookService {

    private final StorageService storage;
    private final Path path;
    private AddressBookDocument document;
    private CompletableFuture<Void> mutationTail = CompletableFuture.completedFuture(null);

    public JsonAddressBookService(StorageService storage, Path path) {
        this.storage = storage;
        this.path = path;
        this.document = storage.load(path, AddressBookDocument.class, AddressBookDocument::new).join();
        validate(document);
    }

    private void validate(AddressBookDocument candidate) {
        candidate.players.forEach((uuid, entry) -> {
            UUID.fromString(uuid);
            entry.name = requireValue(entry.name, "entry.name");
            entry.address = IpAddresses.normalize(entry.address)
                    .orElseThrow(() -> new IllegalStateException("Invalid stored IP address"));
        });
    }

    private String requireValue(String value, String name) {
        var normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return normalized;
    }

    @Override
    public synchronized CompletableFuture<Void> remember(UUID uuid, String name, String address) {
        var normalizedName = requireValue(name, "name");
        var normalizedAddress = IpAddresses.normalize(address)
                .orElseThrow(() -> new IllegalArgumentException("Invalid IP address"));
        var result = new CompletableFuture<Void>();
        mutationTail = mutationTail.handle((ignored, failure) -> null)
                .thenCompose(ignored -> {
                    AddressBookDocument next;
                    synchronized (this) {
                        next = copy(document);
                    }
                    var entry = new AddressBookDocument.Entry();
                    entry.name = normalizedName;
                    entry.address = normalizedAddress;
                    next.players.put(uuid.toString(), entry);
                    return storage.save(path, next).whenComplete((saved, failure) -> {
                        if (failure == null) {
                            synchronized (this) {
                                document = next;
                            }
                            result.complete(null);
                        } else {
                            result.completeExceptionally(failure);
                        }
                    });
                });
        mutationTail.whenComplete((ignored, failure) -> {
            if (failure != null) result.completeExceptionally(failure);
        });
        return result;
    }

    @Override
    public synchronized Optional<String> address(UUID uuid) {
        var entry = document.players.get(uuid.toString());
        return entry == null ? Optional.empty() : Optional.of(entry.address);
    }

    @Override
    public synchronized Optional<String> address(String name) {
        var normalized = requireValue(name, "name").toLowerCase(Locale.ROOT);
        return document.players.values().stream()
                .filter(entry -> entry.name.toLowerCase(Locale.ROOT).equals(normalized))
                .map(entry -> entry.address)
                .findFirst();
    }

    private AddressBookDocument copy(AddressBookDocument source) {
        var target = new AddressBookDocument();
        source.players.forEach((uuid, existing) -> {
            var entry = new AddressBookDocument.Entry();
            entry.name = existing.name;
            entry.address = existing.address;
            target.players.put(uuid, entry);
        });
        return target;
    }

}
