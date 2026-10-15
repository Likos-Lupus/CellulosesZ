package top.likoslupus.cellulosesz.application.command

import top.likoslupus.cellulosesz.core.command.help.EffectiveCommandRoutes
import top.likoslupus.cellulosesz.core.command.registry.CommandRegistrationReport

/**
 * Holds only the latest command-registration report. It is instance state owned by the composition
 * root (never a process-wide mutable registry) and exposes a read-only projection for help ownership
 * and admin diagnostics. No Brigadier node, dispatcher or player is retained.
 */
internal class CommandRegistrationDiagnostics {

    @Volatile private var latest: CommandRegistrationReport? = null

    /** The literals CellulosesZ actually installed in the most recent generation. */
    val effectiveRoutes: EffectiveCommandRoutes = EffectiveCommandRoutes {
        latest?.installedLiterals ?: emptySet()
    }

    fun publish(report: CommandRegistrationReport) {
        latest = report
    }

    fun snapshot(): CommandRegistrationReport? =
        latest

}
