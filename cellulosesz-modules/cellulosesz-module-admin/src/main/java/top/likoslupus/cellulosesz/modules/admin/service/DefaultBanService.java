package top.likoslupus.cellulosesz.modules.admin.service;

import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerConnectionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.common.admin.*;
import top.likoslupus.cellulosesz.common.text.PlayerAudienceService;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminStatus;

import java.net.InetAddress;
import java.time.Clock;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class DefaultBanService implements BanService {

    private final BanPlatformService platform;
    private final PlayerDirectory players;
    private final PlayerConnectionService connections;
    private final PlayerAudienceService audiences;
    private final PermissionService permissions;
    private final Clock clock;

    public DefaultBanService(
            BanPlatformService platform,
            PlayerDirectory players,
            PlayerConnectionService connections,
            PlayerAudienceService audiences,
            PermissionService permissions,
            Clock clock
    ) {
        this.platform = requireNonNull(platform, "platform");
        this.players = requireNonNull(players, "players");
        this.connections = requireNonNull(connections, "connections");
        this.audiences = requireNonNull(audiences, "audiences");
        this.permissions = requireNonNull(permissions, "permissions");
        this.clock = requireNonNull(clock, "clock");
    }

    @Override
    public AdminResult ban(
            UUID targetId,
            String targetName,
            AdminActor actor,
            String reason
    ) {
        var target = new PlayerProfileId(targetId, targetName);
        final BanPlatformResult result;

        try {
            result = platform.banUser(new BanUserRequest(
                    target,
                    toPlatform(actor),
                    reason,
                    clock.instant(),
                    Expiration.permanent()
            ));
        } catch (IllegalArgumentException failure) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.ban-failed",
                    MessageArguments.builder().add(targetName).build()
            );
        }

        if (!result.status().successful()) {
            return failure(
                    result,
                    "service.admin.ban-failed",
                    MessageArguments.builder().add(targetName).build()
            );
        }

        var disconnected = platform.disconnectMatchingPlayers(
                BanDisconnectRequest.user(targetId, reason)
        );

        return disconnected.status().successful()
                ?
                AdminResult.success(
                        "service.admin.ban-success",
                        MessageArguments.builder().add(targetName).build()
                )
                : AdminResult.partial(
                        "service.admin.ban-success",
                        MessageArguments.builder().add(targetName).build()
                );
    }

    @Override
    public AdminResult unban(
            UUID targetId,
            String targetName,
            AdminActor actor
    ) {
        var result = platform.pardonUser(new PlayerProfileId(targetId, targetName));
        return result.status().successful()
                ?
                AdminResult.success(
                        "service.admin.unban-success",
                        MessageArguments.builder().add(targetName).build()
                )
                : failure(
                        result,
                        "service.admin.unban-failed",
                        MessageArguments.builder().add(targetName).build()
                );
    }

    @Override
    public AdminResult banIp(
            InetAddress target,
            AdminActor actor,
            String reason
    ) {
        var canonical = IpAddresses.canonical(target);
        final BanPlatformResult result;

        try {
            result = platform.banIp(new BanIpRequest(
                    target,
                    toPlatform(actor),
                    reason,
                    clock.instant(),
                    Expiration.permanent()
            ));
        } catch (IllegalArgumentException failure) {
            return AdminResult.failure(
                    AdminStatus.INVALID_INPUT,
                    "service.admin.ban-ip-failed",
                    MessageArguments.builder().add(canonical).build()
            );
        }

        if (!result.status().successful()) {
            return failure(
                    result,
                    "service.admin.ban-ip-failed",
                    MessageArguments.builder().add(canonical).build()
            );
        }

        var disconnected = platform.disconnectMatchingPlayers(
                BanDisconnectRequest.address(target, reason)
        );

        return disconnected.status().successful()
                ?
                AdminResult.success(
                        "service.admin.ban-ip-success",
                        MessageArguments.builder().add(canonical).build()
                )
                : AdminResult.partial(
                        "service.admin.ban-ip-success",
                        MessageArguments.builder().add(canonical).build()
                );
    }

    @Override
    public AdminResult unbanIp(InetAddress target, AdminActor actor) {
        var canonical = IpAddresses.canonical(target);
        var result = platform.pardonIp(target);

        return result.status().successful()
                ?
                AdminResult.success(
                        "service.admin.unban-ip-success",
                        MessageArguments.builder().add(canonical).build()
                )
                : failure(
                        result,
                        "service.admin.unban-ip-failed",
                        MessageArguments.builder().add(canonical).build()
                );
    }

    @Override
    public AdminResult kick(CellPlayer target, String reason) {
        var result = connections.disconnect(target, RichText.plain(reason));
        return result.successful()
                ?
                AdminResult.success(
                        "service.admin.kick-success",
                        MessageArguments.builder().add(target.name()).build()
                )
                : AdminResult.failure(
                        AdminStatus.PLATFORM_FAILURE,
                        "service.admin.kick-failed",
                        MessageArguments.builder().add(target.name()).build()
                );
    }

    private static BanActor toPlatform(AdminActor actor) {
        return new BanActor(
                actor.uuid().orElse(null),
                actor.name()
        );
    }

    private static AdminResult failure(
            BanPlatformResult result,
            String key,
            MessageArguments arguments
    ) {
        var status = switch (result.status()) {
            case ALREADY_BANNED -> AdminStatus.ALREADY_EXISTS;
            case NOT_FOUND -> AdminStatus.NOT_FOUND;
            case PERSISTENCE_FAILURE -> AdminStatus.PERSISTENCE_FAILURE;
            case NOT_READY, WRONG_THREAD, PLATFORM_FAILURE -> AdminStatus.PLATFORM_FAILURE;
            case SUCCESS -> throw new IllegalArgumentException("success is not a failure");
        };
        return AdminResult.failure(status, key, arguments);
    }

    public PlayerDirectory players() {
        return players;
    }

    public PermissionService permissions() {
        return permissions;
    }

    public PlayerAudienceService audiences() {
        return audiences;
    }

}
