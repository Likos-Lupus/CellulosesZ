package top.likoslupus.cellulosesz.modules.world.command;

import top.likoslupus.cellulosesz.api.platform.PlatformService;
import top.likoslupus.cellulosesz.api.world.TreeType;
import top.likoslupus.cellulosesz.api.world.WorldPlatformService;
import top.likoslupus.cellulosesz.modules.world.config.WorldConfig;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class BigTreeCommand extends AbstractTreeCommand {

    public BigTreeCommand(
            PlatformService platform,
            WorldPlatformService worlds,
            WorldConfig config
    ) {
        super(
                platform,
                worlds,
                config,
                Set.of(
                        TreeType.LARGE_OAK,
                        TreeType.LARGE_SPRUCE,
                        TreeType.LARGE_JUNGLE,
                        TreeType.DARK_OAK
                ),
                TreeType.LARGE_OAK
        );
    }

    @Override
    public String permission() {
        return "cellulosesz.command.bigtree";
    }

    @Override
    public String usage() {
        return "/bigtree [tree|oak|redwood|spruce|jungle|darkoak]";
    }

    @Override
    public String name() {
        return "bigtree";
    }

    @Override
    protected Optional<TreeType> parseType(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "tree", "oak" -> Optional.of(TreeType.LARGE_OAK);
            case "redwood", "spruce" -> Optional.of(TreeType.LARGE_SPRUCE);
            case "jungle" -> Optional.of(TreeType.LARGE_JUNGLE);
            case "darkoak" -> Optional.of(TreeType.DARK_OAK);
            default -> Optional.empty();
        };
    }

}
