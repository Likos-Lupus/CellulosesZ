package top.likoslupus.cellulosesz.common.playerstate;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult;
import top.likoslupus.cellulosesz.api.playerstate.GameModeKind;
import top.likoslupus.cellulosesz.api.playerstate.PersonalTimeSetting;
import top.likoslupus.cellulosesz.api.playerstate.PersonalWeatherSetting;
import top.likoslupus.cellulosesz.common.platform.MovementSpeedType;

public interface PlayerStatePlatformService {

    PlatformResult<Integer> seaLevel(CellPlayer player);

    PlatformResult<ExperienceSnapshot> experience(CellPlayer player);

    PlatformResult<ExperienceSnapshot> mutateExperience(
            CellPlayer player,
            ExperienceRequest request
    );

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
            MovementSpeedType type,
            double speed
    );

    PlatformResult<PersonalTimeSetting> setPersonalTime(
            CellPlayer player,
            PersonalTimeSetting setting
    );

    PlatformResult<PersonalWeatherSetting> setPersonalWeather(
            CellPlayer player,
            PersonalWeatherSetting setting
    );

    PlatformResult<Integer> setFireTicks(CellPlayer player, int ticks);

    PlatformResult<Void> extinguish(CellPlayer player);

    PlatformResult<Integer> freeze(CellPlayer player);

    PlatformResult<Void> kill(
            CellPlayer player,
            KillKind kind,
            boolean force
    );

}
