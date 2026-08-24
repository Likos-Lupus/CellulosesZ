package top.likoslupus.cellulosesz.core.permission;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class CompositePermissionBackend implements PermissionBackend {

    private final List<PermissionBackend> backends;

    public CompositePermissionBackend(List<PermissionBackend> backends) {
        this.backends = List.copyOf(backends);
    }

    @Override
    public boolean has(CellPlayer player, String permission) {
        return backends.stream().anyMatch(backend ->
                backend.has(player, permission)
        );
    }

    @Override
    public int intOption(CellPlayer player, String key, int fallback) {
        return backends.stream()
                .map(backend -> backend.stringOption(player, key))
                .filter(Objects::nonNull)
                .mapToInt(value -> parseInteger(key, value))
                .findFirst()
                .orElse(fallback);
    }

    private static int parseInteger(String key, String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new IllegalArgumentException(
                    "Permission option " + key + " is not an integer: " + value,
                    failure
            );
        }
    }

    @Override
    public boolean boolOption(CellPlayer player, String key, boolean fallback) {
        return backends.stream()
                .map(backend -> backend.stringOption(player, key))
                .filter(Objects::nonNull)
                .map(value -> parseBoolean(key, value))
                .findFirst()
                .orElse(fallback);
    }

    private static boolean parseBoolean(String key, String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException(
                    "Permission option " + key + " is not a boolean: " + value
            );
        };
    }

    @Override
    public String stringOption(CellPlayer player, String key) {
        return backends.stream()
                .map(backend -> backend.stringOption(player, key))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

}
