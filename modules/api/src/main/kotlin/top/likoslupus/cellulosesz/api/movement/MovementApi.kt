package top.likoslupus.cellulosesz.api.movement

/** Groups the Movement domain: homes, warps, spawn, teleport, requests and their events. */
public interface MovementApi {

    public val homes: HomesApi

    public val warps: WarpsApi

    public val spawn: SpawnApi

    public val teleport: TeleportApi

    public val requests: TeleportRequestApi

    public val events: MovementEvents

}
