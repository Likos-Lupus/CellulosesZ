package top.likoslupus.cellulosesz.fabric

import net.fabricmc.api.ModInitializer
import top.likoslupus.cellulosesz.application.bootstrap.CellulosesZ

object CellulosesZFabric : ModInitializer {

    override fun onInitialize() {
        CellulosesZ.initialize()
    }

}
