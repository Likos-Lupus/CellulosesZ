package top.likoslupus.cellulosesz.fabric;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.sign.SignBreakRequest;
import top.likoslupus.cellulosesz.api.sign.SignPlatformService;
import top.likoslupus.cellulosesz.api.sign.SignSnapshot;
import top.likoslupus.cellulosesz.api.sign.SignWriteRequest;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayerUnavailableException;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;
import top.likoslupus.cellulosesz.common.world.MinecraftWorlds;
import top.likoslupus.cellulosesz.fabric.event.FabricPlatformEventBridge;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.util.Objects.requireNonNull;

public final class FabricSignOperations implements SignPlatformService {

    private final MinecraftServerHandle server;

    public FabricSignOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<SignSnapshot> target(CellPlayer player, int maximumDistance) {
        if (maximumDistance < 1) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INVALID_ARGUMENT,
                    "Distance must be positive"
            );
        }

        return onServerThread(() -> {
            var nativePlayer = MinecraftPlayers.requireOnline(server, player);
            var hit = nativePlayer.pick(maximumDistance, 0.0F, false);
            if (!(hit instanceof BlockHitResult blockHit)
                    || hit.getType() != HitResult.Type.BLOCK
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.TARGET_NOT_FOUND,
                        "No sign is targeted"
                );
            }

            var blockEntity = nativePlayer.level().getBlockEntity(blockHit.getBlockPos());
            if (!(blockEntity instanceof SignBlockEntity sign)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.TARGET_NOT_FOUND,
                        "Target is not a sign"
                );
            }

            var front = sign.isFacingFrontText(nativePlayer);
            return PlatformResult.success(snapshot(
                    nativePlayer.level().dimension().identifier().toString(),
                    blockHit.getBlockPos(),
                    sign,
                    front
            ));
        });
    }

    @Override
    public PlatformResult<SignSnapshot> compareAndReplace(SignWriteRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var level = MinecraftWorlds.findLoaded(
                    server.requireRunning(),
                    request.location().world()
            );

            if (level.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.WORLD_NOT_FOUND,
                        "World is not loaded"
                );
            }

            var position = blockPosition(request.location());
            var blockEntity = level.orElseThrow().getBlockEntity(position);
            if (!(blockEntity instanceof SignBlockEntity sign)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.TARGET_NOT_FOUND,
                        "Target is no longer a sign"
                );
            }

            if (sign.isWaxed() && !request.allowWaxed()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.PERMISSION_DENIED,
                        "Sign is waxed"
                );
            }

            if (!sameLines(sign, request.front(), request.expectedLines())) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Sign changed before commit"
                );
            }

            var text = request.front()
                    ? sign.getFrontText()
                    : sign.getBackText();
            for (var line = 0; line < 4; line++) {
                text = text.setMessage(line, component(request.replacementLines().get(line)));
            }

            if (!sign.setText(text, request.front())) {
                return PlatformResult.failure(
                        PlatformOperationStatus.STATE_NOT_ALLOWED,
                        "Sign rejected replacement text"
                );
            }

            sign.setChanged();
            var state = level.orElseThrow().getBlockState(position);
            level.orElseThrow().sendBlockUpdated(position, state, state, 3);

            return PlatformResult.success(snapshot(
                    request.location().world(),
                    position,
                    sign,
                    request.front()
            ));
        });
    }

    @Override
    public PlatformResult<Void> compareAndBreak(SignBreakRequest request) {
        requireNonNull(request, "request");
        return onServerThread(() -> {
            var level = MinecraftWorlds.findLoaded(
                    server.requireRunning(),
                    request.location().world()
            );

            if (level.isEmpty()) {
                return PlatformResult.failure(
                        PlatformOperationStatus.WORLD_NOT_FOUND,
                        "World is not loaded"
                );
            }

            var position = blockPosition(request.location());
            var blockEntity = level.orElseThrow().getBlockEntity(position);
            if (!(blockEntity instanceof SignBlockEntity sign)) {
                return PlatformResult.failure(
                        PlatformOperationStatus.TARGET_NOT_FOUND,
                        "Target is no longer a sign"
                );
            }

            if (!sameLines(sign, true, request.expectedFrontLines())
                    || !sameLines(sign, false, request.expectedBackLines())
            ) {
                return PlatformResult.failure(
                        PlatformOperationStatus.CONFLICT,
                        "Sign changed before break"
                );
            }

            var broken = FabricPlatformEventBridge.withoutSignBreakCheck(() ->
                    level.orElseThrow().destroyBlock(
                            position,
                            true,
                            MinecraftPlayers.requireOnline(server, request.actor())
                    )
            );

            return broken
                    ? PlatformResult.success()
                    : PlatformResult.failure(
                            PlatformOperationStatus.STATE_NOT_ALLOWED,
                            "Sign could not be broken"
                    );
        });
    }

    private static BlockPos blockPosition(CellLocation location) {
        if (!Double.isFinite(location.x())
                || !Double.isFinite(location.y())
                || !Double.isFinite(location.z())
        ) {
            throw new IllegalArgumentException("Sign location must contain finite coordinates");
        }

        return BlockPos.containing(location.x(), location.y(), location.z());
    }

    private static boolean sameLines(
            SignBlockEntity sign,
            boolean front,
            List<String> expected
    ) {
        var actual = (
                front
                        ? sign.getFrontText()
                        : sign.getBackText()
        ).getMessages(false);

        if (actual.length != expected.size()) {
            return false;
        }

        return IntStream.range(0, actual.length)
                .allMatch(index ->
                        normalize(actual[index].getString()).equals(normalize(expected.get(index)))
                );
    }

    private static Component component(String value) {
        if (value.length() >= 2 && value.charAt(0) == '§') {
            var formatting = ChatFormatting.getByCode(value.charAt(1));
            if (formatting != null) {
                return Component.literal(value.substring(2)).withStyle(formatting);
            }
        }

        return Component.literal(value);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .replaceAll("(?i)[§&][0-9A-FK-OR]", "")
                .strip();
    }

    private <T> PlatformResult<T> onServerThread(Supplier<PlatformResult<T>> operation) {
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Operation requires the server thread"
            );
        }

        try {
            return operation.get();
        } catch (MinecraftPlayerUnavailableException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    failure.getMessage()
            );
        } catch (RuntimeException failure) {
            return PlatformResult.failure(
                    PlatformOperationStatus.INTERNAL_ERROR,
                    failure.getClass().getSimpleName()
            );
        }
    }

    private static SignSnapshot snapshot(
            String world,
            BlockPos position,
            SignBlockEntity sign,
            boolean front
    ) {
        var messages = (
                front
                        ? sign.getFrontText()
                        : sign.getBackText()
        ).getMessages(false);

        return new SignSnapshot(
                new CellLocation(
                        world,
                        position.getX(),
                        position.getY(),
                        position.getZ(),
                        0.0F,
                        0.0F
                ),
                front,
                sign.isWaxed(),
                Arrays.stream(messages)
                        .map(Component::getString)
                        .toList()
        );
    }

}
