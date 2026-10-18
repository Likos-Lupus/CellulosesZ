package top.likoslupus.cellulosesz.application.bootstrap

import java.util.concurrent.atomic.AtomicBoolean

/**
 * The only composition root. It explicitly wires config, runtime, storage, shared identity lookup and
 * the bounded-context facades; it contains no domain logic.
 */
public object CellulosesZ {

    private val initialized = AtomicBoolean(false)

    public fun initialize(platform: PlatformServices) {
        if (!initialized.compareAndSet(false, true)) {
            return
        }

        // Each phase fills the shared graph in the original startup order: config/storage, features
        // (administration -> public-API provider -> movement -> communication -> utility ->
        // player -> economy), API event forwarding, commands, then lifecycle listeners.
        val state = BootstrapConfiguration.create(platform)
        BootstrapFeatures.install(state)
        BootstrapApi.forwardEvents(state)
        BootstrapFeatures.installIntegration(state)
        BootstrapCommands.install(state)
        BootstrapLifecycle.register(state)
    }

}
