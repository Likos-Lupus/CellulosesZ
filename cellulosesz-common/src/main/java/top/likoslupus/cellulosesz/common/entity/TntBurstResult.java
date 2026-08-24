package top.likoslupus.cellulosesz.common.entity;

public record TntBurstResult(
        int requested,
        int spawned
) {

    public TntBurstResult {
        if (requested < 1 || spawned < 0 || spawned > requested) {
            throw new IllegalArgumentException("invalid TNT counts");
        }
    }

}
