package top.likoslupus.cellulosesz.fabric.hook;

import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import top.likoslupus.cellulosesz.api.item.ItemAutomationService;
import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.service.ServiceRegistry;
import top.likoslupus.cellulosesz.api.sign.SignService;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;
import top.likoslupus.cellulosesz.api.text.LocaleResolver;
import top.likoslupus.cellulosesz.api.text.MessageRenderer;

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

public final class FabricGameplayHooks {

    private final ServiceRegistry services;
    private final PlatformService platform;
    private final MessageRenderer renderer;
    private final LocaleResolver locales;
    private final Map<UUID, Set<String>> pendingUnlimited = new ConcurrentHashMap<>();

    public FabricGameplayHooks(
            ServiceRegistry services,
            PlatformService platform,
            MessageRenderer renderer,
            LocaleResolver locales
    ) {
        this.services = services;
        this.platform = platform;
        this.renderer = renderer;
        this.locales = locales;
    }

    public void register() {
        UseBlockCallback.EVENT.register(this::useBlock);
        UseItemCallback.EVENT.register(this::useItem);
        AttackBlockCallback.EVENT.register((player, level, hand, _, _) -> {
            if (level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND
                    || !(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
            return usePowerTool(serverPlayer, "");
        });
        AttackEntityCallback.EVENT.register((player, level, hand, target, _) -> {
            if (level.isClientSide()
                    || hand != InteractionHand.MAIN_HAND
                    || !(player instanceof ServerPlayer serverPlayer)
                    || !(target instanceof ServerPlayer targetPlayer)) return InteractionResult.PASS;
            return usePowerTool(serverPlayer, targetPlayer.getGameProfile().name());
        });
    }

    private InteractionResult useBlock(
            Player player,
            Level level,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (level.isClientSide()
                || hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer serverPlayer)
        ) return InteractionResult.PASS;

        queueUnlimited(serverPlayer);
        return useSign(serverPlayer, level, hit);
    }

    private InteractionResult useItem(
            Player player,
            Level level,
            InteractionHand hand
    ) {
        if (!level.isClientSide()
                && hand == InteractionHand.MAIN_HAND
                && player instanceof ServerPlayer serverPlayer
        ) queueUnlimited(serverPlayer);
        return InteractionResult.PASS;
    }

    private InteractionResult usePowerTool(ServerPlayer player, String clickedPlayerName) {
        var automation = services.optional(ItemAutomationService.class);
        if (automation.isEmpty()) return InteractionResult.PASS;

        var wrapped = platform.player(player);
        return wrapped
                .filter(cellPlayer ->
                        automation.get().executePowerTool(cellPlayer, clickedPlayerName)
                )
                .<InteractionResult>map(_ -> InteractionResult.SUCCESS)
                .orElse(InteractionResult.PASS);
    }

    private void queueUnlimited(ServerPlayer nativePlayer) {
        var automation = services.optional(ItemAutomationService.class);
        if (automation.isEmpty()) return;

        var wrapped = platform.player(nativePlayer);
        if (wrapped.isEmpty()) return;

        platform.heldItemId(wrapped.get())
                .filter(itemId -> automation.get().unlimited(wrapped.get().uuid(), itemId))
                .ifPresent(itemId -> pendingUnlimited
                        .computeIfAbsent(
                                wrapped.get().uuid(),
                                _ -> ConcurrentHashMap.newKeySet()
                        )
                        .add(itemId)
                );
    }

    private InteractionResult useSign(
            ServerPlayer player,
            Level level,
            BlockHitResult hit
    ) {
        var signs = services.optional(SignService.class);
        if (signs.isEmpty()) return InteractionResult.PASS;

        var blockEntity = level.getBlockEntity(hit.getBlockPos());
        if (!(blockEntity instanceof SignBlockEntity sign)) return InteractionResult.PASS;

        var wrapped = platform.player(player);
        if (wrapped.isEmpty()) return InteractionResult.PASS;

        var location = new CellLocation(
                level.dimension().identifier().toString(),
                hit.getBlockPos().getX() + 0.5D,
                hit.getBlockPos().getY(),
                hit.getBlockPos().getZ() + 0.5D,
                player.getYRot(),
                player.getXRot()
        );
        var front = sign.isFacingFrontText(player);
        var signLines = lines(
                (front
                        ? sign.getFrontText()
                        : sign.getBackText()
                ).getMessages(false)
        );
        var execution = signs.get().use(
                wrapped.get(),
                location,
                front,
                signLines,
                player.isShiftKeyDown()
        );
        if (!execution.handled()) return InteractionResult.PASS;

        execution.result().whenComplete((result, failure) ->
                platform.runOnServerThread(() -> {
                    if (failure != null) {
                        platform.sendMessage(
                                wrapped.get(),
                                renderer.render(
                                        locales.locale(wrapped.get()),
                                        "service.sign.execution-failed",
                                        Map.of("reason", safeReason(failure))
                                )
                        );
                        return;
                    }
                    result.optionalMessage().ifPresent(message ->
                            platform.sendMessage(
                                    wrapped.get(),
                                    renderer.render(
                                            locales.locale(wrapped.get()),
                                            message.key(),
                                            message.placeholders()
                                    )
                            )
                    );
                })
        );
        return InteractionResult.SUCCESS;
    }

    private List<String> lines(Component[] messages) {
        return Arrays.stream(messages)
                .map(Component::getString)
                .toList();
    }

    private String safeReason(Throwable throwable) {
        var cause = throwable;
        while ((cause instanceof CompletionException || cause instanceof ExecutionException)
                && cause.getCause() != null
        ) {
            cause = cause.getCause();
        }

        var message = cause.getMessage();
        return message == null || message.isBlank()
                ? cause.getClass().getSimpleName()
                : message;
    }

    public void tick(MinecraftServer server) {
        if (pendingUnlimited.isEmpty()) return;

        var pending = new LinkedHashMap<>(pendingUnlimited);
        pendingUnlimited.clear();
        services.optional(ItemAutomationService.class)
                .ifPresent(automation ->
                        pending.forEach((uuid, itemIds) -> {
                            var nativePlayer = server.getPlayerList().getPlayer(uuid);
                            if (nativePlayer == null) return;

                            platform.player(nativePlayer)
                                    .ifPresent(player ->
                                            itemIds.forEach(itemId ->
                                                    automation.maintainUnlimited(player, itemId)
                                            )
                                    );
                        })
                );
    }

}
