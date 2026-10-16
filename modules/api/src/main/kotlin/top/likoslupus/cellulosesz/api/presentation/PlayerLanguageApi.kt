package top.likoslupus.cellulosesz.api.presentation

import top.likoslupus.cellulosesz.api.ApiQuery
import java.util.*

/**
 * The effective language a player sees: their explicit preference, or the server default. It never
 * exposes a client's system locale or locale guessing.
 */
public interface PlayerLanguageApi {

    public fun effective(playerId: UUID): ApiQuery<String>

}
