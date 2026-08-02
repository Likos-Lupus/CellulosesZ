package top.likoslupus.cellulosesz.modules.home.application;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.home.HomeRenameStatus;
import top.likoslupus.cellulosesz.api.home.HomeService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;
import top.likoslupus.cellulosesz.modules.home.HomeConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultHomeCommandServiceTest {

    private static final UUID UUID_ONE = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final CellPlayer PLAYER = new CellPlayer(
            UUID_ONE,
            "Tester"
    );
    private static final CellLocation LOCATION = new CellLocation(
            "minecraft:overworld",
            1, 64, 2,
            0, 0
    );

    @Test
    void defaultHomeTeleportsAndStartsCooldownOnlyAfterSuccess() {
        var homes = new FakeHomes();
        homes.values.put("home", LOCATION);

        var teleports = new FakeTeleports(true);
        var cooldowns = new FakeCooldowns();
        var service = service(
                homes,
                teleports,
                cooldowns,
                true,
                new HomeConfig()
        );

        var result = service.teleport(
                request(false, false),
                ""
        ).join();

        assertTrue(result.success());
        assertEquals("home", homes.lastLookup);
        assertEquals(1, teleports.calls.get());
        assertEquals(3, teleports.lastOptions.warmupSeconds());
        assertTrue(teleports.lastOptions.safe());
        assertEquals(1, cooldowns.starts.get());
    }

    private static DefaultHomeCommandService service(
            FakeHomes homes,
            FakeTeleports teleports,
            FakeCooldowns cooldowns,
            boolean online,
            HomeConfig config
    ) {
        return new DefaultHomeCommandService(
                homes,
                teleports,
                cooldowns,
                resolver(online),
                _ -> LOCATION,
                new ImmediateExecutor(),
                config
        );
    }

    private static HomeCommandService.Request request(
            boolean bypassCooldown,
            boolean bypassWarmup
    ) {
        return new HomeCommandService.Request(
                UUID_ONE,
                "Tester",
                bypassCooldown,
                bypassWarmup
        );
    }

    @NullMarked
    private static PlayerResolver resolver(boolean online) {
        return new PlayerResolver() {
            @Override
            public ResolvedPlayer resolveKnown(String input, @Nullable CellPlayer viewer) {
                return new ResolvedPlayer(
                        online
                                ? ResolvedPlayerState.ONLINE
                                : ResolvedPlayerState.OFFLINE,
                        UUID_ONE,
                        "Tester",
                        online
                                ? PLAYER
                                : null,
                        false
                );
            }

            @Override
            public ResolvedPlayer resolveKnown(UUID uuid, @Nullable CellPlayer viewer) {
                return resolveKnown("Tester", viewer);
            }

            @Override
            public CompletableFuture<ResolvedPlayer> resolve(
                    String input,
                    @Nullable CellPlayer viewer
            ) {
                return CompletableFuture.completedFuture(resolveKnown(input, viewer));
            }
        };
    }

    @Test
    void cooldownWarmupBypassesAndTeleportFailurePreserveSemantics() {
        var homes = new FakeHomes();
        homes.values.put("home", LOCATION);

        var cooldowns = new FakeCooldowns();
        cooldowns.remaining = Duration.ofSeconds(2);

        var blockedTeleports = new FakeTeleports(true);
        var service = service(
                homes,
                blockedTeleports,
                cooldowns,
                true,
                new HomeConfig()
        );

        assertFalse(service.teleport(
                request(false, false),
                "home"
        ).join().success());
        assertEquals(0, blockedTeleports.calls.get());

        var failedTeleports = new FakeTeleports(false);
        var bypassed = service(
                homes,
                failedTeleports,
                cooldowns,
                true,
                new HomeConfig()
        );
        var result = bypassed.teleport(
                request(true, true),
                "home"
        ).join();

        assertFalse(result.success());
        assertEquals(0, failedTeleports.lastOptions.warmupSeconds());
        assertEquals(0, cooldowns.starts.get());

        var exceptionalTeleports = new FakeTeleports(true, true);
        var exceptionalResult = service(
                homes,
                exceptionalTeleports,
                cooldowns,
                true,
                new HomeConfig()
        ).teleport(
                request(true, true),
                "home"
        ).join();

        assertFalse(exceptionalResult.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_TELEPORT_REQUEST_FAILED,
                exceptionalResult.message().key()
        );
        assertEquals(0, cooldowns.starts.get());
    }

    @Test
    void playerGoingOfflineBeforeFutureCompletionStopsTeleport() {
        var homes = new FakeHomes();
        homes.values.put("home", LOCATION);

        var teleports = new FakeTeleports(true);
        var service = service(
                homes,
                teleports,
                new FakeCooldowns(),
                false,
                new HomeConfig()
        );

        var result = service.teleport(
                request(false, false),
                "home"
        ).join();

        assertFalse(result.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_COMMON_PLAYER_OFFLINE,
                result.message().key()
        );
        assertEquals(0, teleports.calls.get());
    }

    @Test
    void setValidatesNameLimitAndOnlyRepliesAfterPersistence() {
        var config = new HomeConfig();
        config.limits.defaultMaxHomes = 1;
        config.naming.minLength = 2;
        config.naming.maxLength = 5;

        var homes = new FakeHomes();
        homes.values.put("old", LOCATION);

        var service = service(
                homes,
                new FakeTeleports(true),
                new FakeCooldowns(),
                true,
                config
        );

        assertFalse(service.set(
                request(false, false),
                "x",
                false
        ).join().success());
        assertFalse(service.set(
                request(false, false),
                "new",
                false
        ).join().success());
        assertTrue(service.set(
                request(false, false),
                "new",
                true
        ).join().success());
        assertEquals("new", homes.lastSet);

        homes.failSet = true;
        assertFalse(service.set(
                request(false, false),
                "other",
                true
        ).join().success());
    }

    @Test
    void deleteAndAtomicRenameDistinguishAllStatuses() {
        var homes = new FakeHomes();
        homes.values.put("old", LOCATION);

        var service = service(
                homes,
                new FakeTeleports(true),
                new FakeCooldowns(),
                true,
                new HomeConfig()
        );

        assertFalse(service.delete(UUID_ONE, "missing").join().success());
        assertTrue(service.delete(UUID_ONE, "old").join().success());

        homes.values.put("old", LOCATION);
        homes.renameStatus = HomeRenameStatus.SOURCE_MISSING;
        assertEquals(
                GeneratedMessageKeys.COMMANDS_HOME_RENAME_HOME_COMMAND_ERROR_SOURCE_MISSING,
                service.rename(UUID_ONE, "old", "new").join().message().key()
        );

        homes.renameStatus = HomeRenameStatus.TARGET_EXISTS;
        assertEquals(
                GeneratedMessageKeys.COMMANDS_HOME_RENAME_HOME_COMMAND_ERROR_TARGET_EXISTS,
                service.rename(UUID_ONE, "old", "new").join().message().key()
        );

        homes.renameStatus = HomeRenameStatus.RENAMED;
        assertTrue(service.rename(UUID_ONE, "old", "new").join().success());
        assertEquals(Set.of("old"), service.cachedNames(UUID_ONE));
    }

    @NullMarked
    private static final class FakeHomes implements HomeService {

        private final Map<String, CellLocation> values = new LinkedHashMap<>();
        private String lastLookup = "";
        private String lastSet = "";
        private boolean failSet;
        private HomeRenameStatus renameStatus = HomeRenameStatus.RENAMED;

        @Override
        public CompletableFuture<Map<String, CellLocation>> homes(UUID uuid) {
            return CompletableFuture.completedFuture(Map.copyOf(values));
        }

        @Override
        public Map<String, CellLocation> cachedHomes(UUID uuid) {
            return Map.copyOf(values);
        }

        @Override
        public CompletableFuture<Optional<CellLocation>> home(UUID uuid, String name) {
            lastLookup = name;
            return CompletableFuture.completedFuture(Optional.ofNullable(values.get(name.toLowerCase())));
        }

        @Override
        public CompletableFuture<Boolean> setHome(
                UUID uuid,
                String name,
                CellLocation location
        ) {
            if (failSet) {
                return CompletableFuture.failedFuture(new IllegalStateException("save"));
            }

            lastSet = name;
            values.put(name.toLowerCase(), location);
            return CompletableFuture.completedFuture(true);
        }

        @Override
        public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
            return CompletableFuture.completedFuture(values.remove(name.toLowerCase()) != null);
        }

        @Override
        public CompletableFuture<HomeRenameStatus> renameHomeDetailed(
                UUID uuid,
                String oldName,
                String newName
        ) {
            return CompletableFuture.completedFuture(renameStatus);
        }

    }

    @NullMarked
    private static final class FakeTeleports implements TeleportService {

        private final boolean succeed;
        private final boolean exceptional;
        private final AtomicInteger calls = new AtomicInteger();
        private @Nullable TeleportOptions lastOptions;

        private FakeTeleports(boolean succeed) {
            this(succeed, false);
        }

        private FakeTeleports(boolean succeed, boolean exceptional) {
            this.succeed = succeed;
            this.exceptional = exceptional;
        }

        @Override
        public CompletableFuture<TeleportResult> teleport(
                CellPlayer player,
                CellLocation target,
                TeleportOptions options
        ) {
            calls.incrementAndGet();
            lastOptions = options;

            if (exceptional) {
                return CompletableFuture.failedFuture(new IllegalStateException("teleport"));
            }

            return CompletableFuture.completedFuture(succeed
                    ? TeleportResult.success(target)
                    : TeleportResult.failed(
                            top.likoslupus.cellulosesz.api.teleport.TeleportStatus.PLATFORM_FAILURE,
                            "service.teleport.failed"
                    ));
        }

        @Override
        public boolean cancelWarmup(UUID uuid, TeleportStatus status) {
            return false;
        }

        @Override
        public boolean warmingUp(UUID uuid) {
            return false;
        }

        @Override
        public CompletableFuture<Void> rememberBackLocation(CellPlayer player) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> rememberBackLocation(UUID uuid, CellLocation location) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Optional<CellLocation> backLocation(UUID uuid) {
            return Optional.empty();
        }

        @Override
        public void shutdown() {
        }

    }

    @NullMarked
    private static final class FakeCooldowns implements CooldownService {

        private final AtomicInteger starts = new AtomicInteger();
        private Duration remaining = Duration.ZERO;

        @Override
        public Duration remaining(UUID uuid, String key) {
            return remaining;
        }

        @Override
        public boolean ready(UUID uuid, String key) {
            return remaining.isZero();
        }

        @Override
        public void start(
                UUID uuid,
                String key,
                Duration duration
        ) {
            starts.incrementAndGet();
        }

        @Override
        public void clear(UUID uuid, String key) {
        }

    }

    @NullMarked
    private static final class ImmediateExecutor implements ServerThreadExecutor {

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
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        }

    }

}
