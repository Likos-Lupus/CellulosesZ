package top.likoslupus.cellulosesz.core.player

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

/** Resolves online players from server-thread state; holds no domain state of its own. */
public object PlayerResolver {

    public fun onlineById(server: MinecraftServer, id: UUID): ServerPlayer? =
        server.playerList.getPlayer(id)

    public fun onlineByName(server: MinecraftServer, name: String): ServerPlayer? =
        server.playerList.getPlayerByName(name)

    public fun online(server: MinecraftServer): List<ServerPlayer> =
        server.playerList.players

}
