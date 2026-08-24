package top.likoslupus.cellulosesz.common.admin;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

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
