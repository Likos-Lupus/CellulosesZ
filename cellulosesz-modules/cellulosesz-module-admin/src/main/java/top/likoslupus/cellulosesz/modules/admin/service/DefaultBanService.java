package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;

import java.util.Map;

public final class DefaultBanService implements BanService {

    private final PlatformService platform;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final PermissionService permissions;

    public DefaultBanService(
            PlatformService platform,
            MessageRenderer renderer,
            LocaleResolver locales,
            PermissionService permissions
    ) {
        this.platform = platform;
        this.renderer = renderer;
        this.locales = locales;
        this.permissions = permissions;
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
        var normalized = IpAddresses.normalize(target);
        if (normalized.isEmpty()) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.invalid-address",
                    Map.of("address", target)
            );
        }

        var address = normalized.orElseThrow();
        return command(
                "ban-ip %s%s".formatted(address, suffix(reason)),
                "service.admin.ban-ip-success",
                "service.admin.ban-ip-failed",
                Map.of("address", address)
        );
    }

    @Override
    public AdminResult unbanIp(String target, String actor) {
        var normalized = IpAddresses.normalize(target);
        if (normalized.isEmpty()) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.invalid-address",
                    Map.of("address", target)
            );
        }

        var address = normalized.orElseThrow();
        return command(
                "pardon-ip %s".formatted(address),
                "service.admin.unban-ip-success",
                "service.admin.unban-ip-failed",
                Map.of("address", address)
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
            try {
                platform.kick(player.get(), kickReason(player.get(), reason));
            } catch (RuntimeException exception) {
                return AdminResult.failure(
                        "service.admin.kick-failed",
                        Map.of("player", target)
                );
            }

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
        var kicked = 0;
        var exempt = 0;
        var failed = 0;
        for (var player : platform.onlinePlayers()) {
            if (player.name().equalsIgnoreCase(actor)
                    || permissions.has(player.nativeHandle(), "cellulosesz.admin.kickall.exempt")) {
                exempt++;
                continue;
            }
            try {
                platform.kick(player, kickReason(player, reason));
                kicked++;
            } catch (RuntimeException exception) {
                failed++;
            }
        }
        var placeholders = Map.<String, Object>of(
                "kicked", kicked,
                "exempt", exempt,
                "failed", failed
        );
        if (failed > 0 && kicked > 0) {
            return AdminResult.partial("service.admin.kick-all-partial", placeholders);
        }
        if (failed > 0) {
            return AdminResult.failure(
                    AdminStatus.PLATFORM_FAILURE,
                    "service.admin.kick-all-failed",
                    placeholders
            );
        }
        return AdminResult.success("service.admin.kick-all-success", placeholders);
    }

    private String kickReason(CellPlayer player, String reason) {
        if (!reason.isBlank()) return reason;
        return renderer.render(
                locales.locale(player),
                "service.admin.kick-default"
        ).plainText();
    }

    private AdminResult command(
            String command,
            String successKey,
            String failureKey,
            Map<String, ?> placeholders
    ) {
        var result = platform.dispatchNativeConsoleCommand(command);
        return result.success()
                ? AdminResult.success(successKey, placeholders)
                : AdminResult.failure(
                        AdminStatus.NATIVE_COMMAND_FAILURE,
                        failureKey,
                        placeholders
                );
    }

    private String suffix(String reason) {
        return reason.isBlank() ? "" : " " + reason;
    }

}
