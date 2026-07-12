package top.likoslupus.cellulosesz.api.command.service;

import java.util.Map;

public interface PermissionCatalog {

    void register(String permission, String description);

    Map<String, String> permissions();

}
