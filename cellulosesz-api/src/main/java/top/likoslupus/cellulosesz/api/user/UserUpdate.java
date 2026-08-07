package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

public record UserUpdate<T extends @Nullable Object>(
        CellUser user,
        T result
) {

    public UserUpdate {
        requireNonNull(user, "user");
    }

    public static UserUpdate<@Nullable Void> replacing(CellUser user) {
        return new UserUpdate<>(user, null);
    }

    public static <T extends @Nullable Object> UserUpdate<T> of(CellUser user, T result) {
        return new UserUpdate<>(user, result);
    }

}
