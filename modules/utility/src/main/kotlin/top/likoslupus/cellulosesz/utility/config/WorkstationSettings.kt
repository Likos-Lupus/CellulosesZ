package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable
import top.likoslupus.cellulosesz.utility.workstation.WorkstationType

/**
 * Portable workstations are disabled by default because they relax the survival requirement of
 * placing the corresponding block. Owners opt in explicitly.
 */
@Serializable
public data class WorkstationSettings(
    public val enabled: Boolean = false,
    public val allowed: Set<WorkstationType> = WorkstationType.entries.toSet(),
)
