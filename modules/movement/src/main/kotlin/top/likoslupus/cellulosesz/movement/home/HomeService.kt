package top.likoslupus.cellulosesz.movement.home

import top.likoslupus.cellulosesz.movement.config.HomeSettings
import top.likoslupus.cellulosesz.movement.teleport.StoredPosition
import java.util.*

internal sealed interface SetHomeResult {

    data object Success : SetHomeResult

    data object InvalidName : SetHomeResult

    data object LimitReached : SetHomeResult

}

internal sealed interface HomeLookupResult {

    data class Found(val home: Home) : HomeLookupResult

    data object InvalidName : HomeLookupResult

    data object NotFound : HomeLookupResult

}

internal sealed interface DeleteHomeResult {

    data object Deleted : DeleteHomeResult

    data object InvalidName : DeleteHomeResult

    data object NotFound : DeleteHomeResult

}

/** Owns home storage and naming policy only; it never moves a player. */
internal class HomeService(
    private val repository: HomeRepository,
    private val policy: () -> HomeSettings,
) {

    suspend fun set(
        playerId: UUID,
        rawName: String?,
        position: StoredPosition
    ): SetHomeResult {
        val config = policy()
        val name = HomeName.parse(rawName ?: config.defaultName)
            ?: return SetHomeResult.InvalidName

        val existing = repository.list(playerId)
        if (existing.none { it.name == name }
            && existing.size >= config.maxPerPlayer
        ) {
            return SetHomeResult.LimitReached
        }

        repository.put(playerId, Home(name, position))
        return SetHomeResult.Success
    }

    suspend fun get(playerId: UUID, rawName: String?): HomeLookupResult {
        val name = HomeName.parse(rawName ?: policy().defaultName)
            ?: return HomeLookupResult.InvalidName

        val home = repository.list(playerId).firstOrNull { it.name == name }
            ?: return HomeLookupResult.NotFound

        return HomeLookupResult.Found(home)
    }

    suspend fun delete(playerId: UUID, rawName: String): DeleteHomeResult {
        val name = HomeName.parse(rawName)
            ?: return DeleteHomeResult.InvalidName

        return if (repository.remove(playerId, name)) {
            DeleteHomeResult.Deleted
        } else {
            DeleteHomeResult.NotFound
        }
    }

    suspend fun list(playerId: UUID): List<Home> =
        repository.list(playerId)

}
