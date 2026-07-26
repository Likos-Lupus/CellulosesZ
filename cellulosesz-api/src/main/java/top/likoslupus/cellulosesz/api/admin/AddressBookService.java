package top.likoslupus.cellulosesz.api.admin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AddressBookService {

    CompletableFuture<Void> remember(
            UUID uuid,
            String name,
            String address
    );

    Optional<String> address(UUID uuid);

    Optional<String> address(String name);

}
