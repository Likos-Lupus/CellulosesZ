package top.likoslupus.cellulosesz.common.command.source;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.MinecraftCommandResponder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class MinecraftCommandPolicyContext implements CommandPolicyContext {

    private final CommandSourceStack source;
    private final CommandDescriptor descriptor;
    private final PermissionService permissions;
    private final PlatformService platform;
    private final MinecraftCommandResponder responder;
    private final String invokedLabel;
    private final String auditSummary;

    public MinecraftCommandPolicyContext(
            CommandSourceStack source,
            CommandDescriptor descriptor,
            PermissionService permissions,
            PlatformService platform,
            MinecraftCommandResponder responder,
            String invokedLabel,
            String auditSummary
    ) {
        this.source = requireNonNull(source, "source");
        this.descriptor = requireNonNull(descriptor, "descriptor");
        this.permissions = requireNonNull(permissions, "permissions");
        this.platform = requireNonNull(platform, "platform");
        this.responder = requireNonNull(responder, "responder");
        this.invokedLabel = requireNonNull(invokedLabel, "invokedLabel");
        this.auditSummary = requireNonNull(auditSummary, "auditSummary");
    }

    public CommandSourceStack source() {
        return source;
    }

    @Override
    public String invokedLabel() {
        return invokedLabel;
    }

    @Override
    public String canonicalRoot() {
        return descriptor.canonicalName();
    }

    @Override
    public boolean player() {
        return source.getEntity() instanceof ServerPlayer;
    }

    @Override
    public Optional<UUID> playerUuid() {
        return source.getEntity() instanceof ServerPlayer p
                ? Optional.of(p.getUUID())
                : Optional.empty();
    }

    @Override
    public Optional<String> playerName() {
        return source.getEntity() instanceof ServerPlayer p
                ? Optional.of(p.getGameProfile().name())
                : Optional.empty();
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.has(source, permission);
    }

    @Override
    public String auditSummary() {
        return auditSummary;
    }

    @Override
    public void reply(LocalizedMessage message) {
        responder.reply(source, message);
    }

    @Override
    public void error(LocalizedMessage message) {
        responder.error(source, message);
    }

    public Optional<CellPlayer> currentPlayer() {
        var stableUuid = playerUuid();
        if (stableUuid.isEmpty()) return Optional.empty();
        return platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(stableUuid.orElseThrow()))
                .findFirst();
    }

    public int intPermissionOption(String key, int fallback) {
        return permissions.intOption(source, key, fallback);
    }

    public void respondAll(boolean success, List<LocalizedMessage> messages) {
        messages.forEach(message -> respond(success, message));
    }

    public void respond(boolean success, LocalizedMessage message) {
        if (success) reply(message);
        else error(message);
    }

}
