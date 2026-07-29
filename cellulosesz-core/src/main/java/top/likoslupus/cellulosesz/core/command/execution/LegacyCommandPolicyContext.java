package top.likoslupus.cellulosesz.core.command.execution;

import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class LegacyCommandPolicyContext implements CommandPolicyContext {

    private final CommandDescriptor descriptor;
    private final CommandInvocation invocation;

    public LegacyCommandPolicyContext(
            CommandDescriptor descriptor,
            CommandInvocation invocation
    ) {
        this.descriptor = requireNonNull(descriptor, "descriptor");
        this.invocation = requireNonNull(invocation, "invocation");
    }

    @Override
    public String invokedLabel() {
        return invocation.label();
    }

    @Override
    public String canonicalRoot() {
        return descriptor.canonicalName();
    }

    @Override
    public boolean player() {
        return invocation.player();
    }

    @Override
    public Optional<UUID> playerUuid() {
        return invocation.playerUuid();
    }

    @Override
    public Optional<String> playerName() {
        return invocation.playerName();
    }

    @Override
    public boolean hasPermission(String permission) {
        return invocation.hasPermission(permission);
    }

    @Override
    public String auditSummary() {
        return invocation.auditSummary();
    }

    @Override
    public void reply(LocalizedMessage message) {
        invocation.reply(message);
    }

    @Override
    public void error(LocalizedMessage message) {
        invocation.error(message);
    }

}
