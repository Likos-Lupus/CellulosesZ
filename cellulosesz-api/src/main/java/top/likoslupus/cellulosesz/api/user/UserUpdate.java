package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public record UserUpdate<T>(
        CellUser user,
        @Nullable T result
) {

    public UserUpdate {
        requireNonNull(user, "user");
    }

    public static UserUpdate<Void> replacing(CellUser user) {
        return new UserUpdate<>(user, null);
    }

    public static <T> UserUpdate<T> of(CellUser user, @Nullable T result) {
        return new UserUpdate<>(user, result);
    }

}
