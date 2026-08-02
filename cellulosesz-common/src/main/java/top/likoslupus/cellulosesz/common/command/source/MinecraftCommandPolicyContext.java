package top.likoslupus.cellulosesz.common.command.source;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import top.likoslupus.cellulosesz.api.command.execution.CommandDescriptor;
import top.likoslupus.cellulosesz.api.command.execution.CommandPolicyContext;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.common.command.MinecraftCommandResponder;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class MinecraftCommandPolicyContext implements CommandPolicyContext {

    private final CommandSourceStack source;
    private final CommandDescriptor descriptor;
    private final PermissionService permissions;
    private final PlayerDirectory players;
    private final MinecraftCommandResponder responder;
    private final String invokedLabel;
    private final String auditSummary;

    public MinecraftCommandPolicyContext(
            CommandSourceStack source,
            CommandDescriptor descriptor,
            PermissionService permissions,
            PlayerDirectory players,
            MinecraftCommandResponder responder,
            String invokedLabel,
            String auditSummary
    ) {
        this.source = requireNonNull(source, "source");
        this.descriptor = requireNonNull(descriptor, "descriptor");
        this.permissions = requireNonNull(permissions, "permissions");
        this.players = requireNonNull(players, "players");
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
        return source.getEntity() instanceof ServerPlayer player
                ? Optional.of(player.getUUID())
                : Optional.empty();
    }

    @Override
    public Optional<String> playerName() {
        return source.getEntity() instanceof ServerPlayer player
                ? Optional.of(player.getGameProfile().name())
                : Optional.empty();
    }

    @Override
    public boolean hasPermission(String permission) {
        if (permission.isBlank()) {
            return true;
        }

        if (source.getEntity() instanceof ServerPlayer player) {
            return permissions.has(MinecraftPlayers.wrap(player), permission);
        }

        return source.permissions().hasPermission(
                new Permission.HasCommandLevel(PermissionLevel.byId(4))
        );
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
        return playerUuid().flatMap(players::onlinePlayer);
    }

    public int intPermissionOption(String key, int fallback) {
        return source.getEntity() instanceof ServerPlayer player
                ? permissions.intOption(MinecraftPlayers.wrap(player), key, fallback)
                : fallback;
    }

    public void respondAll(boolean success, List<LocalizedMessage> messages) {
        messages.forEach(message -> respond(success, message));
    }

    public void respond(boolean success, LocalizedMessage message) {
        if (success) {
            reply(message);
        } else {
            error(message);
        }
    }

}
