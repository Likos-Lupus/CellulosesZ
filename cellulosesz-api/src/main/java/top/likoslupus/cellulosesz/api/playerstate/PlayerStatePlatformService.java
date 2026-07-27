package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface PlayerStatePlatformService {

    PlatformResult<Integer> seaLevel(CellPlayer player);

    PlatformResult<ExperienceSnapshot> experience(CellPlayer player);

    PlatformResult<ExperienceSnapshot> mutateExperience(CellPlayer player, ExperienceRequest request);

    PlatformResult<Void> resetRest(CellPlayer player);

    PlatformResult<Integer> setFireTicks(CellPlayer player, int ticks);

    PlatformResult<Void> extinguish(CellPlayer player);

    PlatformResult<Integer> freeze(CellPlayer player);

    PlatformResult<Void> kill(
            CellPlayer player,
            KillKind kind,
            boolean force
    );

}
