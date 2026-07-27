package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.TreeType;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Set;

public final class TreeCommand extends AbstractTreeCommand {

    public TreeCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            WorldConfig config
    ) {
        super(
                platform,
                worlds,
                config,
                Set.of(
                        TreeType.OAK,
                        TreeType.BIRCH,
                        TreeType.SPRUCE,
                        TreeType.RED_MUSHROOM,
                        TreeType.BROWN_MUSHROOM,
                        TreeType.JUNGLE,
                        TreeType.JUNGLE_BUSH,
                        TreeType.SWAMP
                ),
                TreeType.OAK
        );
    }

    @Override
    public String permission() {
        return "cellulosesz.command.tree";
    }

    @Override
    public String usage() {
        return "/tree [tree|oak|birch|redwood|spruce|redmushroom|brownmushroom|jungle|junglebush|swamp]";
    }

    @Override
    public String name() {
        return "tree";
    }

}
