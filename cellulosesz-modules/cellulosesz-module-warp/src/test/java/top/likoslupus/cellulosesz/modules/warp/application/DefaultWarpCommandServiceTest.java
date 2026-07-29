package top.likoslupus.cellulosesz.modules.warp.application;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.command.service.CooldownService;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.player.PlayerResolver;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayer;
import top.likoslupus.cellulosesz.api.player.ResolvedPlayerState;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.teleport.TeleportOptions;
import top.likoslupus.cellulosesz.api.teleport.TeleportResult;
import top.likoslupus.cellulosesz.api.teleport.TeleportService;
import top.likoslupus.cellulosesz.api.warp.Warp;
import top.likoslupus.cellulosesz.api.warp.WarpService;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;
import top.likoslupus.cellulosesz.modules.warp.WarpConfig;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultWarpCommandServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    private static final CellPlayer PLAYER = new CellPlayer(
            PLAYER_ID,
            "Tester",
            new Object()
    );
    private static final CellLocation LOCATION = new CellLocation(
            "minecraft:overworld",
            5, 70, 8,
            0, 0
    );

    @Test
    void listUsesTypedPageAndFiltersDynamicPermissions() {
        var warps = new FakeWarps();
        warps.values.add(warp("a", "Alpha", "warp.a"));
        warps.values.add(warp("b", "Beta", "warp.b"));
        warps.values.add(warp("c", "Charlie", ""));

        var config = new WarpConfig();
        config.list.pageSize = 1;
        config.list.hideNoPermission = true;

        var service = service(
                warps,
                new FakeTeleports(true),
                new FakeCooldowns(),
                true,
                config
        );

        var pageOne = service.list(
                1,
                permission -> permission.equals("warp.a")
        ).join();
        var pageTwo = service.list(
                2,
                permission -> permission.equals("warp.a")
        ).join();
        var tooHigh = service.list(
                3,
                permission -> permission.equals("warp.a")
        ).join();

        assertTrue(pageOne.success());
        assertEquals("Alpha", pageOne.message().placeholders().get("warps"));

        assertTrue(pageTwo.success());
        assertEquals("Charlie", pageTwo.message().placeholders().get("warps"));

        assertFalse(tooHigh.success());
        assertEquals(GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE, tooHigh.message().key());
    }

    private static Warp warp(
            String name,
            String display,
            String permission
    ) {
        var warp = new Warp(name, LOCATION);
        warp.displayName = display;
        warp.cost = permission;
        return warp;
    }

    private static DefaultWarpCommandService service(
            FakeWarps warps,
            FakeTeleports teleports,
            FakeCooldowns cooldowns,
            boolean online,
            WarpConfig config
    ) {
        return new DefaultWarpCommandService(
                warps,
                teleports,
                cooldowns,
                resolver(online),
                _ -> LOCATION,
                new ImmediateExecutor(),
                config
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
                        PLAYER_ID,
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
            public CompletableFuture<ResolvedPlayer> resolve(String input, @Nullable CellPlayer viewer) {
                return CompletableFuture.completedFuture(resolveKnown(input, viewer));
            }
        };
    }

    @Test
    void teleportChecksDynamicPermissionBeforeServerThreadAndStartsCooldownAfterSuccess() {
        var warps = new FakeWarps();
        warps.values.add(warp("spawn", "Spawn", "warp.spawn"));

        var teleports = new FakeTeleports(true);
        var cooldowns = new FakeCooldowns();
        var service = service(
                warps,
                teleports,
                cooldowns,
                true,
                new WarpConfig()
        );

        var denied = service.teleport(
                request(false, false),
                "spawn",
                _ -> false
        ).join();
        assertFalse(denied.success());
        assertEquals(0, teleports.calls.get());

        var allowed = service.teleport(
                request(false, false),
                "spawn",
                _ -> true
        ).join();
        assertTrue(allowed.success());
        assertEquals(1, teleports.calls.get());
        assertEquals(3, teleports.lastOptions.warmupSeconds());
        assertEquals(1, cooldowns.starts.get());
    }

    private static WarpCommandService.Request request(
            boolean bypassCooldown,
            boolean bypassWarmup
    ) {
        return new WarpCommandService.Request(
                PLAYER_ID,
                "Tester",
                bypassCooldown,
                bypassWarmup
        );
    }

    @Test
    void cooldownBypassWarmupBypassOfflineAndTeleportFailureAreDistinct() {
        var warps = new FakeWarps();
        warps.values.add(warp("spawn", "Spawn", ""));

        var cooldowns = new FakeCooldowns();
        cooldowns.remaining = Duration.ofSeconds(1);

        var teleports = new FakeTeleports(true);
        var service = service(
                warps,
                teleports,
                cooldowns,
                true,
                new WarpConfig()
        );
        assertEquals(
                GeneratedMessageKeys.COMMANDS_WARP_COOLDOWN,
                service.teleport(
                        request(false, false),
                        "spawn",
                        _ -> true
                ).join().message().key()
        );

        var failureTeleports = new FakeTeleports(false);
        var bypass = service(
                warps,
                failureTeleports,
                cooldowns,
                true,
                new WarpConfig()
        );
        assertFalse(bypass.teleport(
                request(true, true),
                "spawn",
                _ -> true
        ).join().success());
        assertEquals(0, failureTeleports.lastOptions.warmupSeconds());
        assertEquals(0, cooldowns.starts.get());

        var exceptionalTeleports = new FakeTeleports(true, true);
        var exceptional = service(
                warps,
                exceptionalTeleports,
                cooldowns,
                true,
                new WarpConfig()
        ).teleport(
                request(true, true),
                "spawn",
                _ -> true
        ).join();
        assertFalse(exceptional.success());
        assertEquals(GeneratedMessageKeys.COMMANDS_TELEPORT_REQUEST_FAILED, exceptional.message().key());
        assertEquals(0, cooldowns.starts.get());

        var offline = service(
                warps,
                new FakeTeleports(true),
                new FakeCooldowns(),
                false,
                new WarpConfig()
        );
        assertEquals(
                GeneratedMessageKeys.COMMANDS_COMMON_PLAYER_OFFLINE,
                offline.teleport(
                        request(false, false),
                        "spawn",
                        _ -> true
                ).join().message().key()
        );
    }

    @Test
    void setDeleteInfoAndSuggestionsPreserveNamesAndFailures() {
        var warps = new FakeWarps();
        warps.values.add(warp("spawn", "Spawn", "warp.spawn"));
        var service = service(
                warps,
                new FakeTeleports(true),
                new FakeCooldowns(),
                true,
                new WarpConfig()
        );

        assertEquals(List.of(), service.usableNames(_ -> false));
        assertEquals(List.of("spawn"), service.usableNames(_ -> true));
        assertTrue(service.info("spawn").join().success());
        assertFalse(service.info("missing").join().success());
        assertTrue(service.set(
                request(false, false),
                "market",
                _ -> true
        ).join().success());
        assertEquals(PLAYER_ID, warps.lastCreator);
        assertTrue(service.delete("market").join().success());
        assertFalse(service.delete("missing").join().success());
    }

    @Test
    void invalidNamesAndPersistenceFailureDoNotReportSuccess() {
        var config = new WarpConfig();
        config.naming.maxLength = 4;

        var warps = new FakeWarps();
        var service = service(
                warps,
                new FakeTeleports(true),
                new FakeCooldowns(),
                true,
                config
        );

        assertFalse(service.set(
                request(false, false),
                "toolong",
                _ -> true
        ).join().success());
        assertFalse(service.delete("bad name").join().success());
        warps.fail = true;
        assertEquals(
                GeneratedMessageKeys.SERVICE_WARP_PERSISTENCE_FAILED,
                service.list(1, _ -> true).join().message().key()
        );
    }

    @NullMarked
    private static final class FakeWarps implements WarpService {

        private final List<Warp> values = new ArrayList<>();
        private boolean fail;
        private @Nullable UUID lastCreator;

        @Override
        public CompletableFuture<List<Warp>> warps() {
            return fail
                    ? CompletableFuture.failedFuture(new IllegalStateException("load"))
                    : CompletableFuture.completedFuture(List.copyOf(values));
        }

        @Override
        public List<Warp> cachedWarps() {
            return List.copyOf(values);
        }

        @Override
        public CompletableFuture<Optional<Warp>> warp(String name) {
            return fail
                    ? CompletableFuture.failedFuture(new IllegalStateException("load"))
                    : CompletableFuture.completedFuture(values.stream()
                            .filter(w -> w.name.equalsIgnoreCase(name))
                            .findFirst());
        }

        @Override
        public Optional<Warp> cachedWarp(String name) {
            return values.stream()
                    .filter(w -> w.name.equalsIgnoreCase(name))
                    .findFirst();
        }

        @Override
        public CompletableFuture<Warp> setWarp(
                String name,
                CellLocation location,
                UUID creator
        ) {
            if (fail) {
                return CompletableFuture.failedFuture(new IllegalStateException("save"));
            }

            lastCreator = creator;
            var created = new Warp(name, location);

            values.removeIf(w -> w.name.equalsIgnoreCase(name));
            values.add(created);
            return CompletableFuture.completedFuture(created);
        }

        @Override
        public CompletableFuture<Boolean> deleteWarp(String name) {
            return fail
                    ? CompletableFuture.failedFuture(new IllegalStateException("delete"))
                    : CompletableFuture.completedFuture(values.removeIf(w -> w.name.equalsIgnoreCase(name)));
        }

        @Override
        public Optional<String> requiredPermission(Warp warp) {
            return warp.cost.isBlank()
                    ? Optional.empty()
                    : Optional.of(warp.cost);
        }

        @Override
        public CompletableFuture<Void> reload() {
            return CompletableFuture.completedFuture(null);
        }

    }

    @NullMarked
    private static final class FakeTeleports implements TeleportService {

        private final boolean successful;
        private final boolean exceptional;
        private final AtomicInteger calls = new AtomicInteger();
        private @Nullable TeleportOptions lastOptions;

        private FakeTeleports(boolean successful) {
            this(successful, false);
        }

        private FakeTeleports(
                boolean successful,
                boolean exceptional
        ) {
            this.successful = successful;
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
            return exceptional
                    ? CompletableFuture.failedFuture(new IllegalStateException("teleport"))
                    : CompletableFuture.completedFuture(
                            successful
                                    ? TeleportResult.success(target)
                                    : TeleportResult.failed("service.teleport.failed", target)
                    );
        }

        @Override
        public boolean cancelWarmup(UUID uuid, String messageKey) {
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
