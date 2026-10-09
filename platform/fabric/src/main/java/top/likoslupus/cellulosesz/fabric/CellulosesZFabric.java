package top.likoslupus.cellulosesz.fabric;

import net.fabricmc.api.ModInitializer;
import top.likoslupus.cellulosesz.application.bootstrap.CellulosesZ;

public final class CellulosesZFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        CellulosesZ.INSTANCE.initialize(new FabricPlatformServices());
    }

}
