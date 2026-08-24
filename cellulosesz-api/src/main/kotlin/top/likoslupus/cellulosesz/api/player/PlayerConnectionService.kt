package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult
import top.likoslupus.cellulosesz.api.text.RichText

public interface PlayerConnectionService {

    public fun disconnect(player: CellPlayer, reason: RichText): PlatformResult<Void>

}
