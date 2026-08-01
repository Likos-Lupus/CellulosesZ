package top.likoslupus.cellulosesz.api.user;

import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public record UserState(
        boolean afk,
        boolean god,
        boolean flying,
        boolean vanished,
        @Nullable String nickname,
        @Nullable Long personalTime,
        @Nullable String personalWeather,
        Map<String, List<String>> powerToolCommands,
        Set<String> unlimitedItems
) {

    public UserState {
        requireNonNull(powerToolCommands, "powerToolCommands");
        requireNonNull(unlimitedItems, "unlimitedItems");
        var commandCopy = new LinkedHashMap<String, List<String>>();
        powerToolCommands.forEach((item, commands) ->
                commandCopy.put(item, List.copyOf(commands))
        );
        powerToolCommands = Map.copyOf(commandCopy);
        unlimitedItems = Set.copyOf(unlimitedItems);
    }

    public static UserState defaults() {
        return new UserState(
                false,
                false,
                false,
                false,
                null,
                null,
                null,
                Map.of(),
                Set.of()
        );
    }

    public UserState withAfk(boolean value) {
        return new UserState(value, god, flying, vanished, nickname, personalTime, personalWeather, powerToolCommands, unlimitedItems);
    }

    public UserState withGod(boolean value) {
        return new UserState(afk, value, flying, vanished, nickname, personalTime, personalWeather, powerToolCommands, unlimitedItems);
    }

    public UserState withFlying(boolean value) {
        return new UserState(afk, god, value, vanished, nickname, personalTime, personalWeather, powerToolCommands, unlimitedItems);
    }

    public UserState withVanished(boolean value) {
        return new UserState(afk, god, flying, value, nickname, personalTime, personalWeather, powerToolCommands, unlimitedItems);
    }

    public UserState withNickname(@Nullable String value) {
        return new UserState(afk, god, flying, vanished, value, personalTime, personalWeather, powerToolCommands, unlimitedItems);
    }

    public UserState withPersonalTime(@Nullable Long value) {
        return new UserState(afk, god, flying, vanished, nickname, value, personalWeather, powerToolCommands, unlimitedItems);
    }

    public UserState withPersonalWeather(@Nullable String value) {
        return new UserState(afk, god, flying, vanished, nickname, personalTime, value, powerToolCommands, unlimitedItems);
    }

    public UserState withPowerToolCommands(Map<String, List<String>> value) {
        return new UserState(afk, god, flying, vanished, nickname, personalTime, personalWeather, value, unlimitedItems);
    }

    public UserState withUnlimitedItems(Set<String> value) {
        return new UserState(afk, god, flying, vanished, nickname, personalTime, personalWeather, powerToolCommands, value);
    }

}
