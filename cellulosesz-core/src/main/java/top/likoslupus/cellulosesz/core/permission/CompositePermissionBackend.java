package top.likoslupus.cellulosesz.core.permission;

import java.util.List;
import java.util.Optional;

public final class CompositePermissionBackend implements PermissionBackend {

    private final List<PermissionBackend> backends;

    public CompositePermissionBackend(List<PermissionBackend> backends) {
        this.backends = List.copyOf(backends);
    }

    @Override
    public boolean has(Object source, String permission) {
        return backends.stream()
                .anyMatch(backend -> backend.has(source, permission));
    }

    @Override
    public int intOption(Object source, String key, int fallback) {
        return backends.stream()
                .flatMap(backend -> backend.stringOption(source, key).stream())
                .flatMap(value -> parseInteger(value).stream())
                .findFirst()
                .orElse(fallback);
    }

    private Optional<Integer> parseInteger(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException _) {
            return Optional.empty();
        }
    }

    @Override
    public boolean boolOption(Object source, String key, boolean fallback) {
        return backends.stream()
                .map(backend -> backend.stringOption(source, key))
                .filter(Optional::isPresent)
                .findFirst()
                .filter(Optional::isPresent)
                .map(value -> Boolean.parseBoolean(value.get()))
                .orElse(fallback);
    }

    @Override
    public Optional<String> stringOption(Object source, String key) {
        return backends.stream()
                .flatMap(backend -> backend.stringOption(source, key).stream())
                .findFirst();
    }

}
