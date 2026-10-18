package top.likoslupus.cellulosesz.application.config

import kotlinx.serialization.Serializable

/**
 * Server backup policy. The command is an argument vector, never a shell string: the process is
 * started directly with [ProcessBuilder], so a configuration cannot inject a shell pipeline. A
 * backup tool's own retention flags live there, not here.
 */
@Serializable
public data class BackupSettings(
    public val enabled: Boolean = false,
    /** Executable plus arguments, for example ["rdiff-backup", "World1", "backups/World1"]. */
    public val command: List<String> = emptyList(),
    /** Periodic interval in minutes; 0 disables the schedule (manual /backup still works). */
    public val intervalMinutes: Int = 30,
    /** Run the scheduled backup even when no player is online. */
    public val alwaysRun: Boolean = false,
    /** Kill the process after this many seconds. */
    public val timeoutSeconds: Int = 300,
    /** Keep only this many characters of captured output in the completion notice. */
    public val maxOutputChars: Int = 2000,
)
