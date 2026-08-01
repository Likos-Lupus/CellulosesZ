package top.likoslupus.cellulosesz.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.*;
import top.likoslupus.cellulosesz.api.platform.admin.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/**
 * Direct adapter for Minecraft's persisted user and IP ban lists.
 */
public final class FabricBanPlatformService implements BanPlatformService {

    private @Nullable MinecraftServer server;

    public void server(MinecraftServer server) {
        this.server = requireNonNull(server, "server");
    }

    public void clearServer() {
        server = null;
    }

    @Override
    public BanPlatformResult banUser(BanUserRequest request) {
        @SuppressWarnings("resource")
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

        if (!save(bans)) {
            rollbackAddedUserBan(bans, target);
            return BanPlatformResult.failure(
                    BanPlatformStatus.PERSISTENCE_FAILURE,
                    "Minecraft could not persist the user ban list"
            );
        }

        return BanPlatformResult.success();
    }

    @Override
    public BanPlatformResult pardonUser(PlayerProfileId target) {
        @SuppressWarnings("resource")
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

        if (!save(bans)) {
            rollbackRemovedUserBan(bans, previous);
            return BanPlatformResult.failure(
                    BanPlatformStatus.PERSISTENCE_FAILURE,
                    "Minecraft could not persist the user ban list"
            );
        }

        return BanPlatformResult.success();
    }

    @Override
    public BanPlatformResult banIp(BanIpRequest request) {
        @SuppressWarnings("resource")
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

        if (!save(bans)) {
            rollbackAddedIpBan(bans, address);
            return BanPlatformResult.failure(
                    BanPlatformStatus.PERSISTENCE_FAILURE,
                    "Minecraft could not persist the IP ban list"
            );
        }

        return BanPlatformResult.success();
    }

    @Override
    public BanPlatformResult pardonIp(InetAddress address) {
        @SuppressWarnings("resource")
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

        if (!save(bans)) {
            rollbackRemovedIpBan(bans, previous);
            return BanPlatformResult.failure(
                    BanPlatformStatus.PERSISTENCE_FAILURE,
                    "Minecraft could not persist the IP ban list"
            );
        }

        return BanPlatformResult.success();
    }

    @Override
    public boolean isUserBanned(PlayerProfileId target) {
        @SuppressWarnings("resource")
        var active = activeServer();
        return active != null
                && active.isSameThread()
                && active.getPlayerList().getBans().get(nameAndId(target)) != null;
    }

    @Override
    public boolean isIpBanned(InetAddress address) {
        @SuppressWarnings("resource")
        var active = activeServer();
        return active != null
                && active.isSameThread()
                && active.getPlayerList().getIpBans().get(canonical(address)) != null;
    }

    @Override
    public BanPlatformResult disconnectMatchingPlayers(BanDisconnectRequest request) {
        @SuppressWarnings("resource")
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
            if (request.userId() != null
                    && request.userId().equals(player.getUUID())
            ) {
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

    private static void rollbackRemovedIpBan(IpBanList bans, IpBanListEntry previous) {
        bans.add(previous);
        save(bans);
    }

    private static String canonical(InetAddress address) {
        var value = address.getHostAddress().toLowerCase(Locale.ROOT);
        var zone = value.indexOf('%');
        return zone < 0
                ? value
                : value.substring(0, zone);
    }

    private static boolean save(IpBanList bans) {
        try {
            bans.save();
            return true;
        } catch (IOException _) {
            return false;
        }
    }

    private static void rollbackAddedIpBan(IpBanList bans, String address) {
        bans.remove(address);
        save(bans);
    }

    private static void rollbackRemovedUserBan(UserBanList bans, UserBanListEntry previous) {
        bans.add(previous);
        save(bans);
    }

    private @Nullable MinecraftServer activeServer() {
        return server;
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

    private static boolean save(UserBanList bans) {
        try {
            bans.save();
            return true;
        } catch (IOException _) {
            return false;
        }
    }

    private static void rollbackAddedUserBan(UserBanList bans, NameAndId target) {
        bans.remove(target);
        save(bans);
    }

}
