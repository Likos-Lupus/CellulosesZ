package top.likoslupus.cellulosesz.api.player

import top.likoslupus.cellulosesz.api.platform.CellPlayer
import top.likoslupus.cellulosesz.api.platform.operation.PlatformResult
import top.likoslupus.cellulosesz.api.text.RichText

public interface DisplayNamePlatformService {

    public fun setDisplayName(
        player: CellPlayer,
        displayName: RichText
    ): PlatformResult<Void>

    public fun refreshPlayerInfo(player: CellPlayer): PlatformResult<Void>

}
