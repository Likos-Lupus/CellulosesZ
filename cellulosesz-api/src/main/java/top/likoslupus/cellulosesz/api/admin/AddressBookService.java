package top.likoslupus.cellulosesz.api.admin;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface AddressBookService {

    CompletableFuture<Void> remember(
            UUID uuid,
            String name,
            InetAddress address
    );

    Optional<InetAddress> address(UUID uuid);

    Optional<InetAddress> address(String name);

}
