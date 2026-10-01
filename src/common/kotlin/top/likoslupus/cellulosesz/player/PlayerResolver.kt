package top.likoslupus.cellulosesz.player

import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

internal object PlayerResolver {

    fun onlineById(server: MinecraftServer, id: UUID): ServerPlayer? =
        server.playerList.getPlayer(id)

    fun onlineByName(server: MinecraftServer, name: String): ServerPlayer? =
        server.playerList.getPlayerByName(name)

    fun online(server: MinecraftServer): List<ServerPlayer> =
        server.playerList.players

}
