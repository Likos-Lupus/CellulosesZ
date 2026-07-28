package top.likoslupus.cellulosesz.modules.admin.service;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.admin.AdminStatus;
import top.likoslupus.cellulosesz.api.permission.PermissionService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.platform.admin.*;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;

import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("DataFlowIssue")
final class DefaultBanServiceTest {

    private static final UUID TARGET_ID = UUID.fromString("00000000-0000-0000-0000-000000000123");

    @Test
    void recordsActorReasonAndDisconnectsAfterFirstUserBan() {
        var platform = new RecordingBanPlatform();
        var service = service(platform);
        var result = service.ban(
                TARGET_ID,
                "Target",
                "Console",
                "Repeated griefing"
        );

        assertEquals(AdminStatus.SUCCESS, result.status());
        assertNotNull(platform.userRequest);
        assertEquals(TARGET_ID, platform.userRequest.target().uuid());
        assertEquals("Target", platform.userRequest.target().name());
        assertEquals("Console", platform.userRequest.actor().name());
        assertEquals("Repeated griefing", platform.userRequest.reason());
        assertNull(platform.userRequest.expiresAt());
        assertNotNull(platform.disconnectRequest);
        assertEquals(TARGET_ID, platform.disconnectRequest.userId());
    }

    private static DefaultBanService service(BanPlatformService bans) {
        return new DefaultBanService(
                proxy(PlatformService.class),
                bans,
                proxy(MessageRenderer.class),
                proxy(LocaleResolver.class),
                proxy(PermissionService.class)
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
                    if (returnType == double.class) return 0.0D;
                    return null;
                }
        );
    }

    @Test
    void duplicateBanDoesNotDisconnectAndMapsToAlreadyExists() {
        var platform = new RecordingBanPlatform();
        platform.banUserResult = BanPlatformResult.failure(BanPlatformStatus.ALREADY_BANNED);
        var service = service(platform);

        var result = service.ban(TARGET_ID, "Target", "Console", "reason");

        assertEquals(AdminStatus.ALREADY_EXISTS, result.status());
        assertNull(platform.disconnectRequest);
    }

    @Test
    void pardonMissingUserMapsToNotFound() {
        var platform = new RecordingBanPlatform();
        platform.pardonUserResult = BanPlatformResult.failure(BanPlatformStatus.NOT_FOUND);

        var result = service(platform).unban(TARGET_ID, "Target", "Console");

        assertEquals(AdminStatus.NOT_FOUND, result.status());
        assertEquals(TARGET_ID, platform.pardonedUser.uuid());
    }

    @Test
    void normalizesIpv6AndDisconnectsMatchingPlayers() throws Exception {
        var platform = new RecordingBanPlatform();

        var result = service(platform).banIp(
                "2001:0db8:0000:0000:0000:0000:0000:0001",
                "Console",
                "proxy abuse"
        );

        assertEquals(AdminStatus.SUCCESS, result.status());
        assertEquals(InetAddress.getByName("2001:db8::1"), platform.ipRequest.target());
        assertEquals("Console", platform.ipRequest.actor().name());
        assertEquals("proxy abuse", platform.ipRequest.reason());
        assertEquals(platform.ipRequest.target(), platform.disconnectRequest.address());
    }

    @Test
    void persistenceFailureIsNotReportedAsSuccess() {
        var platform = new RecordingBanPlatform();
        platform.banIpResult = BanPlatformResult.failure(BanPlatformStatus.PERSISTENCE_FAILURE);

        var result = service(platform).banIp("192.0.2.10", "Console", "reason");

        assertEquals(AdminStatus.PERSISTENCE_FAILURE, result.status());
        assertNull(platform.disconnectRequest);
    }

    @NullMarked
    private static final class RecordingBanPlatform implements BanPlatformService {

        private final BanPlatformResult pardonIpResult = BanPlatformResult.success();
        private final BanPlatformResult disconnectResult = BanPlatformResult.success(1);
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
