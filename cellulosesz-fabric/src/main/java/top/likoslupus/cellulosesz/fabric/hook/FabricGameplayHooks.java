package top.likoslupus.cellulosesz.fabric.hook;

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

import java.util.Arrays;
import java.util.List;

public final class FabricGameplayHooks {

    private final ServiceRegistry services;
    private final PlatformService platform;
    private long ticks;

    public FabricGameplayHooks(
            ServiceRegistry services,
            PlatformService platform
    ) {
        this.services = services;
        this.platform = platform;
    }

    public void register() {
        UseBlockCallback.EVENT.register(this::useBlock);
        UseItemCallback.EVENT.register(this::useItem);
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
        ) {
            return InteractionResult.PASS;
        }

        var signResult = useSign(serverPlayer, level, hit);
        if (signResult != InteractionResult.PASS) return signResult;

        return usePowerTool(serverPlayer);
    }

    private InteractionResult useItem(
            Player player,
            Level level,
            InteractionHand hand
    ) {
        if (level.isClientSide()
                || hand != InteractionHand.MAIN_HAND
                || !(player instanceof ServerPlayer serverPlayer)
        ) {
            return InteractionResult.PASS;
        }
        return usePowerTool(serverPlayer);
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

        var front = lines(sign.getFrontText().getMessages(false));
        var result = signs.get().use(
                wrapped.get(),
                front,
                player.isShiftKeyDown()
        );
        if (!result.handled()) {
            var back = lines(sign.getBackText().getMessages(false));
            result = signs.get().use(
                    wrapped.get(),
                    back,
                    player.isShiftKeyDown()
            );
        }
        if (!result.handled()) return InteractionResult.PASS;

        if (!result.message().isBlank()) platform.sendMessage(wrapped.get(), result.message());
        return InteractionResult.SUCCESS;
    }

    private InteractionResult usePowerTool(ServerPlayer player) {
        var automation = services.optional(ItemAutomationService.class);
        if (automation.isEmpty()) return InteractionResult.PASS;

        var wrapped = platform.player(player);
        return wrapped
                .filter(cellPlayer -> automation.get().executePowerTool(cellPlayer))
                .<InteractionResult>map(_ -> InteractionResult.SUCCESS)
                .orElse(InteractionResult.PASS);
    }

    private List<String> lines(Component[] messages) {
        return Arrays.stream(messages)
                .map(Component::getString)
                .toList();
    }

    public void tick(MinecraftServer server) {
        ticks++;
        if (ticks % 10L != 0L) return;
        services.optional(ItemAutomationService.class).ifPresent(automation ->
                server.getPlayerList().getPlayers()
                        .forEach(nativePlayer ->
                                platform.player(nativePlayer)
                                        .ifPresent(automation::maintainUnlimited)
                        )
        );
    }

}
