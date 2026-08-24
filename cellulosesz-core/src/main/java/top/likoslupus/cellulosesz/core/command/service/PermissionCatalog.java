package top.likoslupus.cellulosesz.core.command.service;

import java.util.Map;

public interface PermissionCatalog {

    void register(String permission, String description);

    Map<String, String> permissions();

}
