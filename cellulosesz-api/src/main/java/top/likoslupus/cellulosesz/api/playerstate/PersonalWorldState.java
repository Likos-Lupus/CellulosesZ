package top.likoslupus.cellulosesz.api.playerstate;

import static java.util.Objects.requireNonNull;

/**
 * Typed personal world overrides, independent of the legacy persisted representation.
 */
public record PersonalWorldState(
        PersonalTimeSetting time,
        PersonalWeatherSetting weather
) {

    public PersonalWorldState {
        requireNonNull(time, "time");
        requireNonNull(weather, "weather");
    }

    public static PersonalWorldState reset() {
        return new PersonalWorldState(PersonalTimeSetting.reset(), PersonalWeatherSetting.RESET);
    }

}
