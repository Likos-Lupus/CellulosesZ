package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.admin.AdminActor;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.admin.Expiration;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.admin.*;
import top.likoslupus.cellulosesz.api.player.PlayerConnectionService;
import top.likoslupus.cellulosesz.api.player.PlayerDirectory;
import top.likoslupus.cellulosesz.api.text.PlayerAudienceService;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultBanServiceTest {

    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private static final AdminActor CONSOLE = AdminActor.console("Console");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-30T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void recordsTypedActorReasonAndDisconnectsAfterFirstUserBan() {
        var platform = new RecordingBanPlatform();
        var result = service(platform).ban(
                TARGET_ID,
                "Target",
                CONSOLE,
                "Repeated griefing"
        );

        assertEquals(AdminStatus.SUCCESS, result.status());
        assertNotNull(platform.userRequest);
        assertEquals(TARGET_ID, platform.userRequest.target().uuid());
        assertEquals("Target", platform.userRequest.target().name());
        assertEquals("Console", platform.userRequest.actor().name());
        assertEquals("Repeated griefing", platform.userRequest.reason());
        assertInstanceOf(Expiration.Permanent.class, platform.userRequest.expiration());
        assertEquals(CLOCK.instant(), platform.userRequest.createdAt());
        assertNotNull(platform.disconnectRequest);
        assertEquals(TARGET_ID, platform.disconnectRequest.userId());
    }

    private static DefaultBanService service(BanPlatformService bans) {
        return new DefaultBanService(
                bans,
                proxy(PlayerDirectory.class),
                proxy(PlayerConnectionService.class),
                proxy(PlayerAudienceService.class),
                proxy(PermissionService.class),
                CLOCK
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (_, method, _) -> {
                    var returnType = method.getReturnType();
                    if (returnType == boolean.class) return false;
                    if (returnType == int.class) return 0;
                    if (returnType == long.class) return 0L;
                    if (returnType == Optional.class) return Optional.empty();
                    if (returnType == List.class) return List.of();
                    return null;
                }
                );
    }

    @Test
    void duplicateBanDoesNotDisconnectAndMapsToAlreadyExists() {
        var platform = new RecordingBanPlatform();
        platform.banUserResult = BanPlatformResult.failure(BanPlatformStatus.ALREADY_BANNED);

        var result = service(platform).ban(
                TARGET_ID,
                "Target",
                CONSOLE,
                "reason"
        );

        assertEquals(AdminStatus.ALREADY_EXISTS, result.status());
        assertNull(platform.disconnectRequest);
    }

    @Test
    void disconnectFailureIsReportedAsPartialSuccess() {
        var platform = new RecordingBanPlatform();
        platform.disconnectResult = BanPlatformResult.failure(BanPlatformStatus.PLATFORM_FAILURE);

        var result = service(platform).ban(
                TARGET_ID,
                "Target",
                CONSOLE,
                "reason"
        );

        assertEquals(AdminStatus.PARTIAL_SUCCESS, result.status());
    }

    @Test
    void pardonMissingUserMapsToNotFound() {
        var platform = new RecordingBanPlatform();
        platform.pardonUserResult = BanPlatformResult.failure(BanPlatformStatus.NOT_FOUND);

        var result = service(platform).unban(TARGET_ID, "Target", CONSOLE);

        assertEquals(AdminStatus.NOT_FOUND, result.status());
        assertEquals(TARGET_ID, platform.pardonedUser.uuid());
    }

    @Test
    void preservesTypedIpv6AndDisconnectsMatchingPlayers() throws Exception {
        var platform = new RecordingBanPlatform();
        var address = InetAddress.getByName("2001:db8::1");

        var result = service(platform).banIp(address, CONSOLE, "proxy abuse");

        assertEquals(AdminStatus.SUCCESS, result.status());
        assertEquals(address, platform.ipRequest.target());
        assertEquals("Console", platform.ipRequest.actor().name());
        assertEquals("proxy abuse", platform.ipRequest.reason());
        assertEquals(address, platform.disconnectRequest.address());
    }

    @Test
    void persistenceFailureIsNotReportedAsSuccess() throws Exception {
        var platform = new RecordingBanPlatform();
        platform.banIpResult = BanPlatformResult.failure(BanPlatformStatus.PERSISTENCE_FAILURE);

        var result = service(platform).banIp(InetAddress.getByName("192.0.2.10"), CONSOLE, "reason");

        assertEquals(AdminStatus.PERSISTENCE_FAILURE, result.status());
        assertNull(platform.disconnectRequest);
    }

    @NullMarked
    private static final class RecordingBanPlatform implements BanPlatformService {

        private final BanPlatformResult pardonIpResult = BanPlatformResult.success();
        private BanPlatformResult disconnectResult = BanPlatformResult.success(1);
        private BanPlatformResult banUserResult = BanPlatformResult.success();
        private BanPlatformResult pardonUserResult = BanPlatformResult.success();
        private BanPlatformResult banIpResult = BanPlatformResult.success();
        private @Nullable BanUserRequest userRequest;
        private @Nullable PlayerProfileId pardonedUser;
        private @Nullable BanIpRequest ipRequest;
        private @Nullable BanDisconnectRequest disconnectRequest;

        @Override
        public BanPlatformResult banUser(BanUserRequest request) {
            userRequest = request;
            return banUserResult;
        }

        @Override
        public BanPlatformResult pardonUser(PlayerProfileId target) {
            pardonedUser = target;
            return pardonUserResult;
        }

        @Override
        public BanPlatformResult banIp(BanIpRequest request) {
            ipRequest = request;
            return banIpResult;
        }

        @Override
        public BanPlatformResult pardonIp(InetAddress address) {
            return pardonIpResult;
        }

        @Override
        public boolean isUserBanned(PlayerProfileId target) {
            return false;
        }

        @Override
        public boolean isIpBanned(InetAddress address) {
            return false;
        }

        @Override
        public BanPlatformResult disconnectMatchingPlayers(BanDisconnectRequest request) {
            disconnectRequest = request;
            return disconnectResult;
        }

    }

}
