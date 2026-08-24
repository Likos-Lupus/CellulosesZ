package top.likoslupus.cellulosesz.common.world;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanListEntry;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.admin.*;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** Direct adapter for Minecraft's persisted user and IP ban lists. */
public final class MinecraftBanPlatformService implements BanPlatformService {

    private final MinecraftServerHandle server;

    public MinecraftBanPlatformService(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public BanPlatformResult banUser(BanUserRequest request) {
        var active = activeServer();

        if (active == null) {
            return notReady();
        }

        if (!active.isSameThread()) {
            return wrongThread();
        }

        var target = nameAndId(request.target());
        var bans = active.getPlayerList().getBans();

        if (bans.get(target) != null) {
            return BanPlatformResult.failure(BanPlatformStatus.ALREADY_BANNED);
        }

        var entry = new UserBanListEntry(
                target,
                date(request.createdAt()),
                request.actor().name(),
                nullableDate(request.expiration().expiresAt().orElse(null)),
                nullableReason(request.reason())
        );
        if (!bans.add(entry)) {
            return BanPlatformResult.failure(
                    BanPlatformStatus.PLATFORM_FAILURE,
                    "Minecraft rejected the user ban entry"
            );
        }

        try {
            bans.save();
        } catch (IOException failure) {
            bans.remove(target);
            try {
                bans.save();
            } catch (IOException rollbackFailure) {
                return persistenceFailure("user ban", failure, rollbackFailure);
            }

            return persistenceFailure("user ban", failure, null);
        }

        return BanPlatformResult.success();
    }

    @Override
    public BanPlatformResult pardonUser(PlayerProfileId target) {
        var active = activeServer();

        if (active == null) {
            return notReady();
        }

        if (!active.isSameThread()) {
            return wrongThread();
        }

        var profile = nameAndId(target);
        var bans = active.getPlayerList().getBans();
        var previous = bans.get(profile);

        if (previous == null) {
            return BanPlatformResult.failure(BanPlatformStatus.NOT_FOUND);
        }

        if (!bans.remove(profile)) {
            return BanPlatformResult.failure(
                    BanPlatformStatus.PLATFORM_FAILURE,
                    "Minecraft rejected the user pardon"
            );
        }

        try {
            bans.save();
        } catch (IOException failure) {
            bans.add(previous);
            try {
                bans.save();
            } catch (IOException rollbackFailure) {
                return persistenceFailure("user pardon", failure, rollbackFailure);
            }

            return persistenceFailure("user pardon", failure, null);
        }

        return BanPlatformResult.success();
    }

    @Override
    public BanPlatformResult banIp(BanIpRequest request) {
        var active = activeServer();

        if (active == null) {
            return notReady();
        }

        if (!active.isSameThread()) {
            return wrongThread();
        }

        var address = canonical(request.target());
        var bans = active.getPlayerList().getIpBans();

        if (bans.get(address) != null) {
            return BanPlatformResult.failure(BanPlatformStatus.ALREADY_BANNED);
        }

        var entry = new IpBanListEntry(
                address,
                date(request.createdAt()),
                request.actor().name(),
                nullableDate(request.expiration().expiresAt().orElse(null)),
                nullableReason(request.reason())
        );
        if (!bans.add(entry)) {
            return BanPlatformResult.failure(
                    BanPlatformStatus.PLATFORM_FAILURE,
                    "Minecraft rejected the IP ban entry"
            );
        }

        try {
            bans.save();
        } catch (IOException failure) {
            bans.remove(address);
            try {
                bans.save();
            } catch (IOException rollbackFailure) {
                return persistenceFailure("IP ban", failure, rollbackFailure);
            }

            return persistenceFailure("IP ban", failure, null);
        }

        return BanPlatformResult.success();
    }

    @Override
    public BanPlatformResult pardonIp(InetAddress address) {
        var active = activeServer();

        if (active == null) {
            return notReady();
        }

        if (!active.isSameThread()) {
            return wrongThread();
        }

        var canonical = canonical(address);
        var bans = active.getPlayerList().getIpBans();
        var previous = bans.get(canonical);

        if (previous == null) {
            return BanPlatformResult.failure(BanPlatformStatus.NOT_FOUND);
        }

        if (!bans.remove(canonical)) {
            return BanPlatformResult.failure(
                    BanPlatformStatus.PLATFORM_FAILURE,
                    "Minecraft rejected the IP pardon"
            );
        }

        try {
            bans.save();
        } catch (IOException failure) {
            bans.add(previous);
            try {
                bans.save();
            } catch (IOException rollbackFailure) {
                return persistenceFailure("IP pardon", failure, rollbackFailure);
            }

            return persistenceFailure("IP pardon", failure, null);
        }

        return BanPlatformResult.success();
    }

    @Override
    public PlatformResult<Boolean> isUserBanned(PlayerProfileId target) {
        var active = activeServer();

        if (active == null) {
            return PlatformResult.failure(PlatformOperationStatus.NOT_READY, "Server is not ready");
        }

        if (!active.isSameThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Ban queries require the server thread"
            );
        }

        return PlatformResult.success(
                active.getPlayerList().getBans().get(nameAndId(target)) != null
        );
    }

    @Override
    public PlatformResult<Boolean> isIpBanned(InetAddress address) {
        var active = activeServer();

        if (active == null) {
            return PlatformResult.failure(PlatformOperationStatus.NOT_READY, "Server is not ready");
        }

        if (!active.isSameThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Ban queries require the server thread"
            );
        }

        return PlatformResult.success(
                active.getPlayerList().getIpBans().get(canonical(address)) != null
        );
    }

    @Override
    public BanPlatformResult disconnectMatchingPlayers(BanDisconnectRequest request) {
        var active = activeServer();

        if (active == null) {
            return notReady();
        }

        if (!active.isSameThread()) {
            return wrongThread();
        }

        var count = 0;

        var component = disconnectReason(request.reason());
        for (var player : active.getPlayerList().getPlayers()) {
            if (request.userId() != null && request.userId().equals(player.getUUID())) {
                player.connection.disconnect(component);
                count++;
                continue;
            }

            if (request.address() != null
                    && player.connection.getRemoteAddress() instanceof InetSocketAddress socket
                    && canonical(request.address()).equals(canonical(socket.getAddress()))
            ) {
                player.connection.disconnect(component);
                count++;
            }
        }

        return BanPlatformResult.success(count);
    }

    private static Component disconnectReason(String reason) {
        return reason.isBlank()
                ? Component.translatable("multiplayer.disconnect.banned.reason.default")
                : Component.literal(reason);
    }

    private static String canonical(InetAddress address) {
        var value = requireNonNull(address, "address").getHostAddress().toLowerCase(Locale.ROOT);
        var zone = value.indexOf('%');
        return zone < 0
                ? value
                : value.substring(0, zone);
    }

    private @Nullable MinecraftServer activeServer() {
        try {
            return server.requireRunning();
        } catch (IllegalStateException _) {
            return null;
        }
    }

    private static BanPlatformResult notReady() {
        return BanPlatformResult.failure(
                BanPlatformStatus.NOT_READY,
                "Minecraft server is not active"
        );
    }

    private static BanPlatformResult wrongThread() {
        return BanPlatformResult.failure(
                BanPlatformStatus.WRONG_THREAD,
                "Ban list operations must run on the Minecraft server thread"
        );
    }

    private static NameAndId nameAndId(PlayerProfileId target) {
        return new NameAndId(target.uuid(), target.name());
    }

    private static Date date(Instant value) {
        return Date.from(value);
    }

    private static @Nullable Date nullableDate(@Nullable Instant value) {
        return value == null
                ? null
                : Date.from(value);
    }

    private static @Nullable String nullableReason(String reason) {
        return reason.isBlank()
                ? null
                : reason;
    }

    private static BanPlatformResult persistenceFailure(
            String operation,
            IOException failure,
            @Nullable IOException rollbackFailure
    ) {
        var detail = "Minecraft could not persist the " + operation + ": "
                + failure.getClass().getSimpleName()
                + (
                failure.getMessage() == null
                        ? ""
                        : " (" + failure.getMessage() + ")"
        );

        if (rollbackFailure != null) {
            detail += "; rollback persistence also failed: "
                    + rollbackFailure.getClass().getSimpleName()
                    + (
                    rollbackFailure.getMessage() == null
                            ? ""
                            : " (" + rollbackFailure.getMessage() + ")"
            );
        }

        return BanPlatformResult.failure(BanPlatformStatus.PERSISTENCE_FAILURE, detail);
    }

}
