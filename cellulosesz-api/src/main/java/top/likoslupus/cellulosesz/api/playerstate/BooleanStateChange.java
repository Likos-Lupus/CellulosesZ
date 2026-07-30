package top.likoslupus.cellulosesz.api.playerstate;

public record BooleanStateChange(
        boolean previous,
        boolean current
) {

}
