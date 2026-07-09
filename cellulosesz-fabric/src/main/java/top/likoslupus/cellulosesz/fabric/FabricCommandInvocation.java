package top.likoslupus.cellulosesz.fabric;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.permission.PermissionService;

import java.util.Optional;

public final class FabricCommandInvocation implements CommandInvocation {

    private final CommandSourceStack source;
    private final PermissionService permissions;
    private final String label;
    private final String[] args;

    public FabricCommandInvocation(
            CommandSourceStack source,
            PermissionService permissions,
            String label,
            String[] args
    ) {
        this.source = source;
        this.permissions = permissions;
        this.label = label;
        this.args = args;
    }

    @Override
    public Object nativeSource() {
        return source;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String[] args() {
        return args.clone();
    }

    @Override
    public boolean player() {
        return source.getEntity() instanceof ServerPlayer;
    }

    @Override
    public Optional<String> playerName() {
        if (source.getEntity() instanceof ServerPlayer player) {
            return Optional.ofNullable(player.getGameProfile().name());
        }
        return Optional.empty();
    }

    @Override
    public boolean hasPermission(String permission) {
        return permissions.has(source, permission);
    }

    @Override
    public void reply(String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }

    @Override
    public void error(String message) {
        source.sendFailure(Component.literal(message));
    }

}
