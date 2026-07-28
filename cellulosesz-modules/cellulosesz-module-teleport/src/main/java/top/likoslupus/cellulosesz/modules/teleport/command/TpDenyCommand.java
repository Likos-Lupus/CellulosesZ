package top.likoslupus.cellulosesz.modules.teleport.command;

import top.likoslupus.cellulosesz.api.command.CellCommand;
import top.likoslupus.cellulosesz.api.command.CommandInvocation;
import top.likoslupus.cellulosesz.api.command.CommandSourceKind;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.teleport.TeleportRequestService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;

import java.util.Map;

public final class TpDenyCommand implements CellCommand {

    private final PlatformService platform;
    private final TeleportRequestService requests;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;

    public TpDenyCommand(
            PlatformService platform,
            TeleportRequestService requests,
            MessageRenderer renderer,
            LocaleResolver locales
    ) {
        this.platform = platform;
        this.requests = requests;
        this.renderer = renderer;
        this.locales = locales;
    }

    @Override
    public String permission() {
        return "cellulosesz.teleport.tpdeny";
    }

    @Override
    public CommandSourceKind sourceKind() {
        return CommandSourceKind.PLAYER_ONLY;
    }

    @Override
    public String usage() {
        return "/tpdeny [request-id|player]";
    }

    @Override
    public String name() {
        return "tpdeny";
    }

    @Override
    public int execute(CommandInvocation invocation) {
        var self = platform.player(invocation);
        if (self.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-deny-command.error.command-can-only-used-by-player");
            return 0;
        }
        if (invocation.args().length > 1) {
            invocation.errorKey("commands.teleport.request.usage", Map.of("usage", usage()));
            return 0;
        }
        var request = invocation.args().length == 0
                ? requests.newestFor(self.orElseThrow().uuid())
                : selected(invocation, self.orElseThrow().uuid(), invocation.args()[0]);
        if (request.isEmpty()) {
            invocation.errorKey("commands.teleport.tp-deny-command.error.there-no-pending-teleport-request");
            return 0;
        }
        var denied = request.orElseThrow();
        if (!requests.remove(denied.id())) {
            invocation.errorKey("commands.teleport.request.changed");
            return 0;
        }
        platform.onlinePlayers().stream()
                .filter(player -> player.uuid().equals(denied.requester()))
                .findFirst()
                .ifPresent(player -> platform.sendMessage(
                        player,
                        renderer.render(
                                locales.locale(player),
                                "commands.teleport.request.denied-by-target",
                                Map.of("player", self.orElseThrow().name())
                        )
                ));
        invocation.replyKey("commands.teleport.tp-deny-command.reply.teleport-request-denied");
        return 1;
    }

    private java.util.Optional<top.likoslupus.cellulosesz.api.teleport.TeleportRequest> selected(
            CommandInvocation invocation,
            java.util.UUID target,
            String token
    ) {
        try {
            var request = requests.pending(java.util.UUID.fromString(token));
            return request.filter(value -> value.target().equals(target));
        } catch (IllegalArgumentException ignored) {
            return invocation.resolvePlayer(token).optionalUuid()
                    .flatMap(uuid -> requests.pendingFor(target, uuid));
        }
    }

}
