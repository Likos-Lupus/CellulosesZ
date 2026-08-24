package top.likoslupus.cellulosesz.common.world;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface WorldPlatformService {

    PlatformResult<ServerDiagnosticsSnapshot> diagnostics();

    PlatformResult<BlockBreakResult> breakTarget(
            CellPlayer player,
            int maximumDistance,
            boolean allowUnbreakable
    );

    PlatformResult<SignTarget> targetSign(CellPlayer player, int maximumDistance);

    PlatformResult<SignTarget> replaceSignText(
            CellPlayer player,
            SignTextMutation mutation,
            boolean allowWaxed
    );

    PlatformResult<SpawnerResult> configureSpawner(
            CellPlayer player,
            int maximumDistance,
            SpawnerRequest request
    );

    PlatformResult<TreeGenerationResult> generateTree(
            CellPlayer player,
            int maximumDistance,
            TreeType type
    );

    PlatformResult<Void> setThunder(String worldId, ThunderRequest request);

    PlatformResult<Void> strikeLightning(LightningRequest request);

}
