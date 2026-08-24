package top.likoslupus.cellulosesz.api.playerstate

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

public interface PlayerStateService {

    public fun setFlying(player: CellPlayer, enabled: Boolean): CompletableFuture<PlayerStateResult>

    public fun setGod(player: CellPlayer, enabled: Boolean): CompletableFuture<PlayerStateResult>

    public fun heal(player: CellPlayer): PlayerStateResult

    public fun feed(player: CellPlayer): PlayerStateResult

    public fun setAfk(
        uuid: UUID,
        name: String,
        afk: Boolean
    ): CompletableFuture<PlayerStateResult>

    public fun afk(uuid: UUID): Boolean

    public fun activity(uuid: UUID, timestamp: Long)

    public fun lastActivity(uuid: UUID): Long

    public fun idleMillis(uuid: UUID): Long

    public fun loadPersonalWorldState(uuid: UUID): CompletableFuture<PersonalWorldState>

    public fun cachedPersonalWorldState(uuid: UUID): PersonalWorldState?

    public fun setPersonalTime(
        player: CellPlayer,
        setting: PersonalTimeSetting
    ): CompletableFuture<PlayerStateResult>

    public fun setPersonalWeather(
        player: CellPlayer,
        setting: PersonalWeatherSetting
    ): CompletableFuture<PlayerStateResult>

    public fun setNick(
        uuid: UUID,
        name: String,
        nickname: String?
    ): CompletableFuture<PlayerStateResult>

    public fun nick(uuid: UUID): String?

}
