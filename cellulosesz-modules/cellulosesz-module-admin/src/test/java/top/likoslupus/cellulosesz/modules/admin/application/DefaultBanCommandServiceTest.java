package top.likoslupus.cellulosesz.modules.admin.application;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.*;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.core.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.modules.admin.config.AdminConfig;
import top.likoslupus.cellulosesz.modules.admin.config.AdminRuntimeSettings;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminActor;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminResult;
import top.likoslupus.cellulosesz.modules.admin.domain.AdminStatus;
import top.likoslupus.cellulosesz.modules.admin.domain.BanRecord;
import top.likoslupus.cellulosesz.modules.admin.service.AddressBookService;
import top.likoslupus.cellulosesz.modules.admin.service.BanService;
import top.likoslupus.cellulosesz.modules.admin.service.TempBanService;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultBanCommandServiceTest {

    private static final UUID TARGET_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String TARGET_NAME = "TargetPlayer";
    private static final AdminActor ACTOR = AdminActor.console("Console");

    @Test
    void unban_whenPermanentSucceedsAndTempFails_returnsPartialSuccess() {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        bans.unbanResult = AdminResult.success(
                "bans.unbanned",
                MessageArguments.builder().add(TARGET_NAME).build()
        );
        tempBans.unbanResult = CompletableFuture.completedFuture(
                AdminResult.failure(
                        AdminStatus.PERSISTENCE_FAILURE,
                        "temp.error",
                        MessageArguments.builder().add(TARGET_NAME).build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unban(TARGET_NAME, ACTOR).join();

        assertEquals(AdminStatus.PARTIAL_SUCCESS, result.status());
        assertEquals("service.admin.unban-partial", result.message().key());
        assertEquals(2, result.components().size());
        assertEquals(AdminStatus.SUCCESS, result.components().get(0).status());
        assertEquals(AdminStatus.PERSISTENCE_FAILURE, result.components().get(1).status());
    }

    private static DefaultBanCommandService createService(
            StubBanService bans,
            StubTempBanService tempBans
    ) {
        return new DefaultBanCommandService(
                bans,
                tempBans,
                new StubPlayerResolver(),
                new StubPlayerDirectory(),
                new StubPlayerNetworkService(),
                new StubAddressBookService(),
                new DirectServerThreadExecutor(),
                new AdminRuntimeSettings(new AdminConfig())
        );
    }

    @Test
    void unban_whenPermanentFailsAndTempSucceeds_returnsPartialSuccess() {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        bans.unbanResult = AdminResult.failure(
                AdminStatus.PLATFORM_FAILURE,
                "bans.failed",
                MessageArguments.builder().add(TARGET_NAME).build()
        );
        tempBans.unbanResult = CompletableFuture.completedFuture(
                AdminResult.success(
                        "temp.unbanned",
                        MessageArguments.builder().add(TARGET_NAME).build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unban(TARGET_NAME, ACTOR).join();

        assertEquals(AdminStatus.PARTIAL_SUCCESS, result.status());
        assertEquals(2, result.components().size());
        assertEquals(AdminStatus.PLATFORM_FAILURE, result.components().get(0).status());
        assertEquals(AdminStatus.SUCCESS, result.components().get(1).status());
    }

    @Test
    void unban_whenBothSucceed_returnsFullSuccess() {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        bans.unbanResult = AdminResult.success(
                "bans.unbanned",
                MessageArguments.builder().add(TARGET_NAME).build()
        );
        tempBans.unbanResult = CompletableFuture.completedFuture(
                AdminResult.success(
                        "temp.unbanned",
                        MessageArguments.builder().add(TARGET_NAME).build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unban(TARGET_NAME, ACTOR).join();

        assertTrue(result.success());
        assertEquals(AdminStatus.SUCCESS, result.status());
        assertEquals("service.admin.unban-success", result.message().key());
        assertEquals(2, result.components().size());
    }

    @Test
    void unban_whenBothNotFound_returnsNotFound() {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        bans.unbanResult = AdminResult.failure(
                AdminStatus.NOT_FOUND,
                "bans.not-found",
                MessageArguments.builder().add(TARGET_NAME).build()
        );
        tempBans.unbanResult = CompletableFuture.completedFuture(
                AdminResult.failure(
                        AdminStatus.NOT_FOUND,
                        "temp.not-found",
                        MessageArguments.builder().add(TARGET_NAME).build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unban(TARGET_NAME, ACTOR).join();

        assertFalse(result.success());
        assertEquals(AdminStatus.NOT_FOUND, result.status());
        assertEquals("service.admin.unban-not-found", result.message().key());
        assertEquals(2, result.components().size());
    }

    @Test
    void unban_whenBothFailTechnically_returnsFailureWithComponents() {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        bans.unbanResult = AdminResult.failure(
                AdminStatus.PLATFORM_FAILURE,
                "bans.plat-error",
                MessageArguments.builder().add(TARGET_NAME).build()
        );
        tempBans.unbanResult = CompletableFuture.completedFuture(
                AdminResult.failure(
                        AdminStatus.PERSISTENCE_FAILURE,
                        "temp.persist-error",
                        MessageArguments.builder().add(TARGET_NAME).build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unban(TARGET_NAME, ACTOR).join();

        assertFalse(result.success());
        assertEquals(AdminStatus.PERSISTENCE_FAILURE, result.status());
        assertEquals("service.admin.unban-failed", result.message().key());
        assertEquals(2, result.components().size());
    }

    @Test
    void unbanIp_whenPermanentSucceedsAndTempFails_returnsPartialSuccess() throws Exception {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        var address = InetAddress.getByName("127.0.0.1");

        bans.unbanIpResult = AdminResult.success(
                "bans.unbanned",
                MessageArguments.builder().add("127.0.0.1").build()
        );
        tempBans.unbanIpResult = CompletableFuture.completedFuture(
                AdminResult.failure(
                        AdminStatus.PERSISTENCE_FAILURE,
                        "temp.error",
                        MessageArguments.builder().add("127.0.0.1").build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unbanIp(address, ACTOR).join();

        assertEquals(AdminStatus.PARTIAL_SUCCESS, result.status());
        assertEquals("service.admin.unban-ip-partial", result.message().key());
        assertEquals(2, result.components().size());
    }

    @Test
    void unbanIp_whenBothFail_returnsFailureStatus() throws Exception {
        var bans = new StubBanService();
        var tempBans = new StubTempBanService();
        var address = InetAddress.getByName("127.0.0.1");

        bans.unbanIpResult = AdminResult.failure(
                AdminStatus.PLATFORM_FAILURE,
                "bans.plat-error",
                MessageArguments.builder().add("127.0.0.1").build()
        );
        tempBans.unbanIpResult = CompletableFuture.completedFuture(
                AdminResult.failure(
                        AdminStatus.PERSISTENCE_FAILURE,
                        "temp.persist-error",
                        MessageArguments.builder().add("127.0.0.1").build()
                )
        );

        var service = createService(bans, tempBans);
        var result = service.unbanIp(address, ACTOR).join();

        assertFalse(result.success());
        assertEquals(AdminStatus.PERSISTENCE_FAILURE, result.status());
        assertEquals("service.admin.unban-ip-failed", result.message().key());
        assertEquals(2, result.components().size());
    }

    private static final class DirectServerThreadExecutor implements ServerThreadExecutor {

        @Override
        public boolean isServerThread() {
            return true;
        }

        @Override
        public void execute(Runnable task) {
            task.run();
        }

        @Override
        public <T> CompletableFuture<T> submit(Supplier<T> task) {
            try {
                return CompletableFuture.completedFuture(task.get());
            } catch (Throwable t) {
                return CompletableFuture.failedFuture(t);
            }
        }

    }

    private static final class StubPlayerResolver implements PlayerResolver {

        @Override
        public ResolvedPlayer resolveKnown(String input, @Nullable CellPlayer viewer) {
            return new ResolvedPlayer(
                    ResolvedPlayerState.ONLINE,
                    TARGET_UUID,
                    TARGET_NAME,
                    null,
                    false
            );
        }

        @Override
        public ResolvedPlayer resolveKnown(UUID uuid, @Nullable CellPlayer viewer) {
            return new ResolvedPlayer(
                    ResolvedPlayerState.ONLINE,
                    TARGET_UUID,
                    TARGET_NAME,
                    null,
                    false
            );
        }

        @Override
        public CompletableFuture<ResolvedPlayer> resolve(
                String input,
                @Nullable CellPlayer viewer
        ) {
            return CompletableFuture.completedFuture(resolveKnown(input, viewer));
        }

    }

    private static final class StubPlayerDirectory implements PlayerDirectory {

        @Override
        public List<CellPlayer> onlinePlayers() {
            return List.of();
        }

        @Override
        public CellPlayer onlinePlayer(UUID uuid) {
            return null;
        }

        @Override
        public CellPlayer onlinePlayer(String name) {
            return null;
        }

        @Override
        public List<String> onlinePlayerNames() {
            return List.of();
        }

    }

    private static final class StubPlayerNetworkService implements PlayerNetworkService {

        @Override
        public InetAddress address(CellPlayer player) {
            return null;
        }

    }

    private static final class StubAddressBookService implements AddressBookService {

        @Override
        public CompletableFuture<Void> remember(UUID uuid, String name, InetAddress address) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Optional<InetAddress> address(UUID player) {
            return Optional.empty();
        }

        @Override
        public Optional<InetAddress> address(String player) {
            return Optional.empty();
        }

    }

    private static final class StubBanService implements BanService {

        AdminResult unbanResult = AdminResult.success("ok");
        AdminResult unbanIpResult = AdminResult.success("ok");

        @Override
        public AdminResult ban(UUID target, String targetName, AdminActor actor, String reason) {
            return AdminResult.success("ok");
        }

        @Override
        public AdminResult unban(UUID target, String targetName, AdminActor actor) {
            return unbanResult;
        }

        @Override
        public AdminResult banIp(InetAddress target, AdminActor actor, String reason) {
            return AdminResult.success("ok");
        }

        @Override
        public AdminResult unbanIp(InetAddress target, AdminActor actor) {
            return unbanIpResult;
        }

        @Override
        public AdminResult kick(CellPlayer target, String reason) {
            return AdminResult.success("ok");
        }

    }

    private static final class StubTempBanService implements TempBanService {

        CompletableFuture<AdminResult> unbanResult = CompletableFuture.completedFuture(AdminResult.success(
                "ok"));
        CompletableFuture<AdminResult> unbanIpResult = CompletableFuture.completedFuture(AdminResult.success(
                "ok"));

        @Override
        public CompletableFuture<AdminResult> tempBan(
                UUID target,
                String targetName,
                AdminActor actor,
                Duration duration,
                String reason
        ) {
            return CompletableFuture.completedFuture(AdminResult.success("ok"));
        }

        @Override
        public CompletableFuture<AdminResult> tempBanIp(
                InetAddress target,
                AdminActor actor,
                Duration duration,
                String reason
        ) {
            return CompletableFuture.completedFuture(AdminResult.success("ok"));
        }

        @Override
        public CompletableFuture<AdminResult> unban(
                UUID target,
                String targetName,
                AdminActor actor
        ) {
            return unbanResult;
        }

        @Override
        public CompletableFuture<AdminResult> unbanIp(InetAddress target, AdminActor actor) {
            return unbanIpResult;
        }

        @Override
        public Optional<BanRecord> active(UUID uuid, String name) {
            return Optional.empty();
        }

        @Override
        public Optional<BanRecord> activeIp(InetAddress address) {
            return Optional.empty();
        }

        @Override
        public CompletableFuture<Integer> purgeExpired() {
            return CompletableFuture.completedFuture(0);
        }

    }

}
