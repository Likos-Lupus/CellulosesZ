package top.likoslupus.cellulosesz.api.command.execution;

import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Optional;
import java.util.UUID;

public interface CommandPolicyContext {

    String invokedLabel();

    String canonicalRoot();

    boolean player();

    Optional<UUID> playerUuid();

    Optional<String> playerName();

    boolean hasPermission(String permission);

    String auditSummary();

    void reply(LocalizedMessage message);

    void error(LocalizedMessage message);

}
