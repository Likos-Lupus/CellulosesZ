package top.likoslupus.cellulosesz.core.command.service;

import top.likoslupus.cellulosesz.api.command.service.PermissionCatalog;

import java.util.LinkedHashMap;
import java.util.Map;

public final class DefaultPermissionCatalog implements PermissionCatalog {

    private final Map<String, String> permissions = new LinkedHashMap<>();

    @Override
    public synchronized void register(String permission, String description) {
        if (permission.isBlank()) return;
        permissions.putIfAbsent(permission, description);
    }

    @Override
    public synchronized Map<String, String> permissions() {
        return Map.copyOf(permissions);
    }

}
