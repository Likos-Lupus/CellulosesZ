package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.text.RichText
import java.util.*

public interface DisplayNameService {

    public fun displayName(player: CellPlayer): RichText

    public fun displayName(uuid: UUID, fallbackName: String): RichText

    public fun plainDisplayName(player: CellPlayer): String

    public fun validNickname(player: CellPlayer, nickname: String): Boolean

    public fun sanitizeNickname(player: CellPlayer, nickname: String): String

    public fun refresh(player: CellPlayer)

    public fun refreshAll()

}
