package top.likoslupus.cellulosesz.api.playerstate;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;

public interface PlayerStatePlatformService {

    PlatformResult<Integer> seaLevel(CellPlayer player);

    PlatformResult<ExperienceSnapshot> experience(CellPlayer player);

    PlatformResult<ExperienceSnapshot> mutateExperience(CellPlayer player, ExperienceRequest request);

    PlatformResult<Void> resetRest(CellPlayer player);

    PlatformResult<Boolean> flying(CellPlayer player);

    PlatformResult<BooleanStateChange> setFlying(CellPlayer player, boolean enabled);

    PlatformResult<Boolean> invulnerable(CellPlayer player);

    PlatformResult<BooleanStateChange> setInvulnerable(CellPlayer player, boolean enabled);

    PlatformResult<Void> heal(CellPlayer player);

    PlatformResult<Void> feed(CellPlayer player);

    PlatformResult<GameModeKind> gameMode(CellPlayer player);

    PlatformResult<GameModeChange> setGameMode(CellPlayer player, GameModeKind mode);

    PlatformResult<MovementSpeedChange> setMovementSpeed(
            CellPlayer player,
            top.likoslupus.cellulosesz.api.platform.MovementSpeedType type,
            double speed
    );

    PlatformResult<PersonalTimeSetting> setPersonalTime(CellPlayer player, PersonalTimeSetting setting);

    PlatformResult<PersonalWeatherSetting> setPersonalWeather(CellPlayer player, PersonalWeatherSetting setting);

    PlatformResult<Integer> setFireTicks(CellPlayer player, int ticks);

    PlatformResult<Void> extinguish(CellPlayer player);

    PlatformResult<Integer> freeze(CellPlayer player);

    PlatformResult<Void> kill(
            CellPlayer player,
            KillKind kind,
            boolean force
    );

}
