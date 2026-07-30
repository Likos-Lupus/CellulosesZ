package top.likoslupus.cellulosesz.api.playerstate;

public sealed interface PersonalTimeSetting permits
        PersonalTimeSetting.Fixed,
        PersonalTimeSetting.Relative,
        PersonalTimeSetting.Reset {

    static PersonalTimeSetting reset() {
        return new Reset();
    }

    record Fixed(
            long ticks
    ) implements PersonalTimeSetting {

    }

    record Relative(
            long offset
    ) implements PersonalTimeSetting {

    }

    record Reset() implements PersonalTimeSetting {

    }

}
