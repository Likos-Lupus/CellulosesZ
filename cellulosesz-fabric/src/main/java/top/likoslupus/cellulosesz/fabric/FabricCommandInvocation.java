package top.likoslupus.cellulosesz.fabric;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.util.Map;
import java.util.Optional;

public final class FabricCommandInvocation implements CommandInvocation {

    private final CommandSourceStack source;
    private final PermissionService permissions;
    private final PlayerResolver players;
    private final @Nullable CellPlayer viewer;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final String label;
    private final String[] args;

    public FabricCommandInvocation(
            CommandSourceStack source,
            PermissionService permissions,
            PlayerResolver players,
            @Nullable CellPlayer viewer,
            MessageRenderer renderer,
            LocaleResolver locales,
            String label,
            String[] args
    ) {
        this.source = source;
        this.permissions = permissions;
        this.players = players;
        this.viewer = viewer;
        this.renderer = renderer;
        this.locales = locales;
        this.label = label;
        this.args = args.clone();
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
    public ResolvedPlayer resolvePlayer(String input) {
        return players.resolveKnown(input, viewer);
    }

    @Override
    public String locale() {
        return locales.locale(this);
    }

    @Override
    public void reply(String message) {
        reply(renderer.renderInline(locale(), "<primary>" + message));
    }

    @Override
    public void reply(RichText message) {
        source.sendSuccess(() -> FabricTextAdapter.toComponent(message), false);
    }

    @Override
    public void replyKey(String key, Map<String, ?> placeholders) {
        reply(renderer.render(locale(), key, placeholders));
    }

    @Override
    public void error(String message) {
        error(renderer.renderInline(locale(), "<red>" + message));
    }

    @Override
    public void error(RichText message) {
        source.sendFailure(FabricTextAdapter.toComponent(message));
    }

    @Override
    public void errorKey(String key, Map<String, ?> placeholders) {
        error(renderer.render(locale(), key, placeholders));
    }

}
