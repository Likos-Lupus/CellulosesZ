package top.likoslupus.cellulosesz.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import top.likoslupus.cellulosesz.application.bootstrap.CellulosesZ;

/**
 * Minimal NeoForge entrypoint. It intentionally contains no business logic and only delegates to
 * the Kotlin composition root so that the loader boundary stays a thin shim. The loader-injected
 * [ModContainer] is forwarded so the platform services can publish the public API as an extension
 * point.
 */
@Mod(CellulosesZNeoForge.MOD_ID)
public final class CellulosesZNeoForge {

    public static final String MOD_ID = "cellulosesz";

    public CellulosesZNeoForge(ModContainer container) {
        CellulosesZ.INSTANCE.initialize(new NeoForgePlatformServices(container));
    }

}
