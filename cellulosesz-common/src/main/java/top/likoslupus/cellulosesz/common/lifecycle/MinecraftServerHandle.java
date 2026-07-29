package top.likoslupus.cellulosesz.common.lifecycle;

import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Lifecycle-owned Minecraft server reference; it never falls back to a worker pool.
 */
public final class MinecraftServerHandle {

    private @Nullable MinecraftServer server;
    private boolean stopping;

    public synchronized void attach(MinecraftServer server) {
        if (this.server != null) {
            throw new IllegalStateException("Minecraft server is already attached");
        }
        this.server = requireNonNull(server, "server");
        this.stopping = false;
    }

    public synchronized void beginStopping(MinecraftServer server) {
        if (this.server == server) {
            stopping = true;
        }
    }

    public synchronized void detach(MinecraftServer server) {
        if (this.server == server) {
            this.server = null;
            this.stopping = true;
        }
    }

    public synchronized Optional<MinecraftServer> current() {
        return Optional.ofNullable(server);
    }

    public synchronized MinecraftServer requireRunning() {
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not attached");
        }
        if (stopping) {
            throw new IllegalStateException("Minecraft server is stopping");
        }
        return server;
    }

}
