package top.likoslupus.cellulosesz.neoforge;

import net.neoforged.fml.common.Mod;
import top.likoslupus.cellulosesz.application.bootstrap.CellulosesZ;

/**
 * Minimal NeoForge entrypoint. It intentionally contains no business logic and only delegates to
 * the Kotlin composition root so that the loader boundary stays a thin shim.
 */
@Mod(CellulosesZNeoForge.MOD_ID)
public final class CellulosesZNeoForge {

    public static final String MOD_ID = "cellulosesz";

    public CellulosesZNeoForge() {
        CellulosesZ.INSTANCE.initialize(new NeoForgePlatformServices());
    }

}
