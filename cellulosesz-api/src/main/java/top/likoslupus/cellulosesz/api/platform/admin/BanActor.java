package top.likoslupus.cellulosesz.api.platform.admin;

import org.jspecify.annotations.Nullable;

import java.util.UUID;

import static top.likoslupus.cellulosesz.api.validation.Checks.*;

public record BanActor(
        @Nullable UUID uuid,
        String name
) {

    public BanActor {
        name = requireNonBlank(name, "name");
        requireMaxLength(name, 64, "name");
        requireNoControlCharacters(name, "name");
    }

    public static BanActor console(String name) {
        return new BanActor(null, name);
    }

}
