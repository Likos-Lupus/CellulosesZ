package top.likoslupus.cellulosesz.movement.random

internal interface RandomTeleportRegionRepository {

    suspend fun list(): List<RandomTeleportRegion>

    suspend fun find(name: RegionName): RandomTeleportRegion?

    suspend fun count(): Int

    suspend fun put(region: RandomTeleportRegion)

    suspend fun remove(name: RegionName): Boolean

}

internal class RandomTeleportDataException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)
