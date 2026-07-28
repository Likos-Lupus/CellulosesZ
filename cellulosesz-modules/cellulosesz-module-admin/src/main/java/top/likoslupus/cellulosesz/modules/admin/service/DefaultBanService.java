package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.admin.AdminResult;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.admin.BanService;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.platform.admin.*;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class DefaultBanService implements BanService {

    private final PlatformService platform;
    private final BanPlatformService banPlatform;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final PermissionService permissions;

    public DefaultBanService(
            PlatformService platform,
            BanPlatformService banPlatform,
            MessageRenderer renderer,
            LocaleResolver locales,
            PermissionService permissions
    ) {
        this.platform = platform;
        this.banPlatform = banPlatform;
        this.renderer = renderer;
        this.locales = locales;
        this.permissions = permissions;
    }

    @Override
    public AdminResult ban(
            UUID targetId,
            String targetName,
            String actor,
            String reason
    ) {
        var target = new PlayerProfileId(targetId, targetName);
        final BanPlatformResult result;
        try {
            result = banPlatform.banUser(new BanUserRequest(
                    target,
                    BanActor.console(actor),
                    reason,
                    Instant.now(),
                    null
            ));
        } catch (IllegalArgumentException exception) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.ban-failed",
                    Map.of("player", targetName)
            );
        }

        if (!result.status().successful()) {
            return failure(result, "service.admin.ban-failed", Map.of("player", targetName));
        }
        var disconnected = banPlatform.disconnectMatchingPlayers(
                BanDisconnectRequest.user(targetId, reason)
        );
        if (!disconnected.status().successful()) {
            return AdminResult.partial("service.admin.ban-success", Map.of("player", targetName));
        }
        return AdminResult.success("service.admin.ban-success", Map.of("player", targetName));
    }

    @Override
    public AdminResult unban(UUID targetId, String targetName, String actor) {
        var result = banPlatform.pardonUser(new PlayerProfileId(targetId, targetName));
        return result.status().successful()
                ? AdminResult.success("service.admin.unban-success", Map.of("player", targetName))
                : failure(result, "service.admin.unban-failed", Map.of("player", targetName));
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
        final InetAddress parsed;
        try {
            parsed = InetAddress.getByName(address);
        } catch (UnknownHostException exception) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.invalid-address",
                    Map.of("address", target)
            );
        }

        final BanPlatformResult result;
        try {
            result = banPlatform.banIp(new BanIpRequest(
                    parsed,
                    BanActor.console(actor),
                    reason,
                    Instant.now(),
                    null
            ));
        } catch (IllegalArgumentException exception) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.ban-ip-failed",
                    Map.of("address", address)
            );
        }
        if (!result.status().successful()) {
            return failure(result, "service.admin.ban-ip-failed", Map.of("address", address));
        }
        var disconnected = banPlatform.disconnectMatchingPlayers(
                BanDisconnectRequest.address(parsed, reason)
        );
        if (!disconnected.status().successful()) {
            return AdminResult.partial("service.admin.ban-ip-success", Map.of("address", address));
        }
        return AdminResult.success("service.admin.ban-ip-success", Map.of("address", address));
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
        final InetAddress parsed;
        try {
            parsed = InetAddress.getByName(address);
        } catch (UnknownHostException exception) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.invalid-address",
                    Map.of("address", target)
            );
        }
        var result = banPlatform.pardonIp(parsed);
        return result.status().successful()
                ? AdminResult.success("service.admin.unban-ip-success", Map.of("address", address))
                : failure(result, "service.admin.unban-ip-failed", Map.of("address", address));
    }

    @Override
    public AdminResult kick(
            String target,
            String actor,
            String reason
    ) {
        var player = platform.onlinePlayer(target);
        if (player.isEmpty()) {
            return AdminResult.failure(
                    AdminStatus.NOT_FOUND,
                    "service.admin.kick-failed",
                    Map.of("player", target)
            );
        }
        try {
            platform.kick(player.orElseThrow(), kickReason(player.orElseThrow(), reason));
        } catch (RuntimeException exception) {
            return AdminResult.failure(
                    AdminStatus.PLATFORM_FAILURE,
                    "service.admin.kick-failed",
                    Map.of("player", target)
            );
        }
        return AdminResult.success(
                "service.admin.kick-success",
                Map.of("player", player.orElseThrow().name())
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

    private static AdminResult failure(
            BanPlatformResult result,
            String messageKey,
            Map<String, ?> placeholders
    ) {
        var status = switch (result.status()) {
            case ALREADY_BANNED -> AdminStatus.ALREADY_EXISTS;
            case NOT_FOUND -> AdminStatus.NOT_FOUND;
            case PERSISTENCE_FAILURE -> AdminStatus.PERSISTENCE_FAILURE;
            case NOT_READY, WRONG_THREAD, PLATFORM_FAILURE -> AdminStatus.PLATFORM_FAILURE;
            case SUCCESS -> throw new IllegalArgumentException("Successful platform result is not a failure");
        };
        return AdminResult.failure(status, messageKey, placeholders);
    }

}
