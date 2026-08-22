package top.likoslupus.cellulosesz.modules.teleport.application;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.execution.ServerThreadExecutor;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.player.*;
import top.likoslupus.cellulosesz.api.teleport.*;
import top.likoslupus.cellulosesz.api.user.CellUser;
import top.likoslupus.cellulosesz.api.user.UserService;
import top.likoslupus.cellulosesz.api.user.UserUpdate;
import top.likoslupus.cellulosesz.api.world.WorldDirectory;
import top.likoslupus.cellulosesz.api.world.WorldResolution;
import top.likoslupus.cellulosesz.modules.teleport.TeleportConfig;
import top.likoslupus.cellulosesz.modules.teleport.TeleportRuntimeSettings;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultTeleportCommandServiceDispatchTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final CellPlayer PLAYER = new CellPlayer(PLAYER_ID, "Alice");
    private static final CellLocation LOCATION = new CellLocation(
            "minecraft:overworld",
            10.0,
            64.0,
            10.0,
            0.0f,
            0.0f
    );

    @Test
    void world_delegatesPlatformMutationThroughServerThreadExecutor() {
        var executor = new GatedServerThreadExecutor();
        var locations = new StubPlayerLocationService();
        var teleports = new StubTeleportService();
        var worlds = new StubWorldDirectory();

        var service = new DefaultTeleportCommandService(
                new StubPlayerDirectory(),
                new StubPlayerResolver(),
                locations,
                new StubTeleportOperations(),
                teleports,
                new StubOfflineLocationService(),
                worlds,
                new StubUserService(),
                executor,
                new TeleportRuntimeSettings(new TeleportConfig())
        );

        var future = service.world(PLAYER, "nether");

        // 1. Assert task was submitted to ServerThreadExecutor
        assertEquals(1, executor.submittedCount.get());

        // 2. Assert NO location query or teleportation occurred BEFORE executor runs
        assertEquals(0, locations.queryCount.get());
        assertEquals(0, teleports.teleportCount.get());
        assertFalse(future.isDone());

        // 3. Run the queued server-thread task
        executor.runNext();

        // 4. Assert mutations now occurred and future completes with success
        assertEquals(1, locations.queryCount.get());
        assertEquals(1, teleports.teleportCount.get());
        assertTrue(future.isDone());
        var result = future.join();
        assertEquals(TeleportCommandStatus.SUCCESS, result.status());
    }

    private static final class GatedServerThreadExecutor implements ServerThreadExecutor {

        final AtomicInteger submittedCount = new AtomicInteger();
        final Queue<Runnable> pendingTasks = new ArrayDeque<>();

        @Override
        public boolean isServerThread() {
            return false;
        }

        @Override
        public void execute(Runnable task) {
            submittedCount.incrementAndGet();
            pendingTasks.add(task);
        }

        @Override
        public <T> CompletableFuture<T> submit(Supplier<T> task) {
            submittedCount.incrementAndGet();
            var future = new CompletableFuture<T>();
            pendingTasks.add(() -> {
                try {
                    future.complete(task.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        }

        void runNext() {
            var task = pendingTasks.poll();
            if (task != null) {
                task.run();
            }
        }

    }

    private static final class StubPlayerLocationService implements PlayerLocationPlatformService {

        final AtomicInteger queryCount = new AtomicInteger();

        @Override
        public CellLocation currentLocation(CellPlayer player) {
            queryCount.incrementAndGet();
            return LOCATION;
        }

    }

    private static final class StubTeleportService implements TeleportService {

        final AtomicInteger teleportCount = new AtomicInteger();

        @Override
        public CompletableFuture<TeleportResult> teleport(
                CellPlayer player,
                CellLocation target,
                TeleportOptions options
        ) {
            teleportCount.incrementAndGet();
            return CompletableFuture.completedFuture(TeleportResult.success(target));
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

    private static final class StubWorldDirectory implements WorldDirectory {

        @Override
        public List<String> loadedWorldIds() {
            return List.of("minecraft:overworld", "minecraft:the_nether");
        }

        @Override
        public WorldResolution resolve(String input) {
            return WorldResolution.resolved("minecraft:the_nether");
        }

    }

    private static final class StubPlayerDirectory implements PlayerDirectory {

        @Override
        public List<CellPlayer> onlinePlayers() {
            return List.of(PLAYER);
        }

        @Override
        public Optional<CellPlayer> onlinePlayer(UUID uuid) {
            return uuid.equals(PLAYER_ID)
                    ? Optional.of(PLAYER)
                    : Optional.empty();
        }

        @Override
        public Optional<CellPlayer> onlinePlayer(String name) {
            return name.equalsIgnoreCase("Alice")
                    ? Optional.of(PLAYER)
                    : Optional.empty();
        }

        @Override
        public List<String> onlinePlayerNames() {
            return List.of("Alice");
        }

    }

    private static final class StubPlayerResolver implements PlayerResolver {

        @Override
        public ResolvedPlayer resolveKnown(String input, @Nullable CellPlayer viewer) {
            return new ResolvedPlayer(
                    ResolvedPlayerState.ONLINE,
                    PLAYER_ID,
                    "Alice",
                    PLAYER,
                    false
            );
        }

        @Override
        public ResolvedPlayer resolveKnown(UUID uuid, @Nullable CellPlayer viewer) {
            return new ResolvedPlayer(
                    ResolvedPlayerState.ONLINE,
                    PLAYER_ID,
                    "Alice",
                    PLAYER,
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

    private static final class StubTeleportOperations implements TeleportOperations {

        @Override
        public PlatformResult<Void> move(CellPlayer player, CellLocation destination) {
            return PlatformResult.success();
        }

        @Override
        public PlatformResult<CellLocation> safeLocation(CellLocation requested) {
            return PlatformResult.success(LOCATION);
        }

        @Override
        public PlatformResult<CellLocation> highestSafeLocation(CellLocation origin) {
            return PlatformResult.success(LOCATION);
        }

        @Override
        public PlatformResult<CellLocation> lowestSafeLocation(CellLocation origin) {
            return PlatformResult.success(LOCATION);
        }

        @Override
        public PlatformResult<CellLocation> targetLocation(CellPlayer player, int maximumDistance) {
            return PlatformResult.success(LOCATION);
        }

    }

    private static final class StubOfflineLocationService implements OfflineLocationService {

        @Override
        public CompletableFuture<Void> remember(UUID uuid, CellLocation location) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public Optional<CellLocation> location(UUID uuid) {
            return Optional.of(LOCATION);
        }

    }

    private static final class StubUserService implements UserService {

        @Override
        public CompletableFuture<CellUser> load(UUID uuid) {
            return CompletableFuture.completedFuture(CellUser.create(uuid));
        }

        @Override
        public CompletableFuture<CellUser> loadFromPlayer(CellPlayer player) {
            return CompletableFuture.completedFuture(CellUser.create(PLAYER_ID));
        }

        @Override
        public Optional<CellUser> cached(UUID uuid) {
            return Optional.of(CellUser.create(uuid));
        }

        @Override
        public Collection<CellUser> cachedUsers() {
            return List.of();
        }

        @Override
        public Optional<UUID> findUuidByName(String name) {
            return Optional.of(PLAYER_ID);
        }

        @Override
        public Collection<UUID> knownUuids() {
            return List.of(PLAYER_ID);
        }

        @Override
        public <T extends @Nullable Object> CompletableFuture<T> update(
                UUID uuid,
                Function<CellUser, UserUpdate<T>> mutation
        ) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> save(UUID uuid) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> saveAll() {
            return CompletableFuture.completedFuture(null);
        }

    }

}
