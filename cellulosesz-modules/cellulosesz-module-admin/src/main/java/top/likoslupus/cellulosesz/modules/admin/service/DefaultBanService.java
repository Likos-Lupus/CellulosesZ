package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;

import java.util.Map;

public final class DefaultBanService implements BanService {

    private final PlatformService platform;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;

    public DefaultBanService(
            PlatformService platform,
            MessageRenderer renderer,
            LocaleResolver locales
    ) {
        this.platform = platform;
        this.renderer = renderer;
        this.locales = locales;
    }

    @Override
    public AdminResult ban(
            String target,
            String actor,
            String reason
    ) {
        return command(
                "ban %s%s".formatted(target, suffix(reason)),
                "service.admin.ban-success",
                "service.admin.ban-failed",
                Map.of("player", target)
        );
    }

    @Override
    public AdminResult unban(String target, String actor) {
        return command(
                "pardon %s".formatted(target),
                "service.admin.unban-success",
                "service.admin.unban-failed",
                Map.of("player", target)
        );
    }

    @Override
    public AdminResult banIp(
            String target,
            String actor,
            String reason
    ) {
        return command(
                "ban-ip %s%s".formatted(target, suffix(reason)),
                "service.admin.ban-ip-success",
                "service.admin.ban-ip-failed",
                Map.of("address", target)
        );
    }

    @Override
    public AdminResult unbanIp(String target, String actor) {
        return command(
                "pardon-ip %s".formatted(target),
                "service.admin.unban-ip-success",
                "service.admin.unban-ip-failed",
                Map.of("address", target)
        );
    }

    @Override
    public AdminResult kick(
            String target,
            String actor,
            String reason
    ) {
        var player = platform.onlinePlayer(target);
        if (player.isPresent()) {
            platform.kick(player.get(), kickReason(player.get(), reason));
            return AdminResult.success(
                    "service.admin.kick-success",
                    Map.of("player", player.get().name())
            );
        }

        return command(
                "kick %s%s".formatted(target, suffix(reason)),
                "service.admin.kick-success",
                "service.admin.kick-failed",
                Map.of("player", target)
        );
    }

    @Override
    public AdminResult kickAll(String actor, String reason) {
        var count = platform.onlinePlayers().stream()
                .mapToInt(player -> {
                    platform.kick(player, kickReason(player, reason));
                    return 1;
                })
                .sum();
        return AdminResult.success(
                "service.admin.kick-all-success",
                Map.of("count", count)
        );
    }

    private String kickReason(CellPlayer player, String reason) {
        if (!reason.isBlank()) return reason;
        return renderer.render(locales.locale(player), "service.admin.kick-default").plainText();
    }

    private AdminResult command(
            String command,
            String successKey,
            String failureKey,
            Map<String, ?> placeholders
    ) {
        return platform.dispatchNativeConsoleCommand(command)
                ? AdminResult.success(successKey, placeholders)
                : AdminResult.failure(failureKey, placeholders);
    }

    private String suffix(String reason) {
        return reason.isBlank() ? "" : " " + reason;
    }

}
