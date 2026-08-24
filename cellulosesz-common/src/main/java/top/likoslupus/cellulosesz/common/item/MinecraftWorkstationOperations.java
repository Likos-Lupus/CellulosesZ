package top.likoslupus.cellulosesz.common.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import top.likoslupus.cellulosesz.api.item.WorkstationKind;
import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformOperationStatus;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.common.lifecycle.MinecraftServerHandle;
import top.likoslupus.cellulosesz.common.player.MinecraftPlayers;

import static java.util.Objects.requireNonNull;

public final class MinecraftWorkstationOperations implements WorkstationPlatformService {

    private final MinecraftServerHandle server;

    public MinecraftWorkstationOperations(MinecraftServerHandle server) {
        this.server = requireNonNull(server, "server");
    }

    @Override
    public PlatformResult<Void> open(CellPlayer player, WorkstationKind kind) {
        requireNonNull(kind, "kind");
        if (!server.serverThread()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.WRONG_THREAD,
                    "Workstations must be opened on the server thread"
            );
        }

        var nativePlayer = MinecraftPlayers.requireOnline(server, player);
        if (!nativePlayer.isAlive() || nativePlayer.hasDisconnected()) {
            return PlatformResult.failure(
                    PlatformOperationStatus.TARGET_NOT_FOUND,
                    "Player disconnected"
            );
        }

        return switch (kind) {
            case ANVIL -> openMenu(nativePlayer, MenuType.ANVIL, "anvil");
            case CARTOGRAPHY -> openMenu(nativePlayer, MenuType.CARTOGRAPHY_TABLE, "cartography");
            case DISPOSAL -> openDisposal(nativePlayer);
            case GRINDSTONE -> openMenu(nativePlayer, MenuType.GRINDSTONE, "grindstone");
            case LOOM -> openMenu(nativePlayer, MenuType.LOOM, "loom");
            case SMITHING -> openMenu(nativePlayer, MenuType.SMITHING, "smithing");
            case STONECUTTER -> openMenu(nativePlayer, MenuType.STONECUTTER, "stonecutter");
            case WORKBENCH -> openMenu(nativePlayer, MenuType.CRAFTING, "crafting");
        };
    }

    private PlatformResult<Void> openMenu(
            ServerPlayer player,
            MenuType<?> type,
            String translationSuffix
    ) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, _) -> requireNonNull(type.create(id, inventory)),
                Component.translatable("container." + translationSuffix)
        ));
        return PlatformResult.success();
    }

    private PlatformResult<Void> openDisposal(ServerPlayer player) {
        var disposal = new SimpleContainer(27) {
            @Override
            public void stopOpen(ContainerUser user) {
                clearContent();
                super.stopOpen(user);
            }
        };

        player.openMenu(new SimpleMenuProvider(
                (id, inventory, _) -> ChestMenu.threeRows(id, inventory, disposal),
                Component.translatable("container.chest")
        ));

        return PlatformResult.success();
    }

}
