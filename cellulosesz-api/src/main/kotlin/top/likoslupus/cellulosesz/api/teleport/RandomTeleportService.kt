package top.likoslupus.cellulosesz.api.teleport

public interface RandomTeleportService {

    public fun randomLocation(
        world: String,
        settings: RandomTeleportSettings
    ): RandomTeleportResult

}
