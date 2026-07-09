package top.likoslupus.cellulosesz.core.permission;

import top.likoslupus.cellulosesz.api.permission.PermissionService;

import java.util.Optional;

public final class DefaultPermissionService implements PermissionService {

    private PermissionBackend backend = (_, permission) -> permission.isBlank();

    public void backend(PermissionBackend backend) {
        this.backend = backend;
    }

    @Override
    public boolean has(
            Object source,
            String permission
    ) {
        if (permission.isBlank()) return true;
        return backend.has(source, permission);
    }

    @Override
    public int intOption(
            Object source,
            String key,
            int fallback
    ) {
        return backend.intOption(source, key, fallback);
    }

    @Override
    public boolean boolOption(
            Object source,
            String key,
            boolean fallback
    ) {
        return backend.boolOption(source, key, fallback);
    }

    @Override
    public Optional<String> stringOption(
            Object source,
            String key
    ) {
        return backend.stringOption(source, key);
    }

}
