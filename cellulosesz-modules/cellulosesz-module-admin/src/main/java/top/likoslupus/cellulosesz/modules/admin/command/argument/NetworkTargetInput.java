package top.likoslupus.cellulosesz.modules.admin.command.argument;

import java.net.InetAddress;

import static java.util.Objects.requireNonNull;

public sealed interface NetworkTargetInput permits
        NetworkTargetInput.Address,
        NetworkTargetInput.PlayerName {

    record Address(InetAddress address) implements NetworkTargetInput {

        public Address {
            requireNonNull(address, "address");
        }

    }

    record PlayerName(String name) implements NetworkTargetInput {

        public PlayerName {
            name = requireNonNull(name, "name").trim();
            if (name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        }

    }

}
