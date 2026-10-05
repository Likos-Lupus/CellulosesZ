package top.likoslupus.cellulosesz.utility.workstation

import top.likoslupus.cellulosesz.utility.config.WorkstationSettings
import java.util.*

internal sealed interface WorkstationResult {

    data object Opened : WorkstationResult
    data object Disabled : WorkstationResult
    data object NotAllowed : WorkstationResult
    data object PlayerOffline : WorkstationResult
    data object Failed : WorkstationResult

}

internal class WorkstationService(
    private val backend: WorkstationBackend,
    private val settings: () -> WorkstationSettings,
) {

    fun open(playerId: UUID, type: WorkstationType): WorkstationResult {
        val config = settings()
        if (!config.enabled) {
            return WorkstationResult.Disabled
        }

        if (type !in config.allowed) {
            return WorkstationResult.NotAllowed
        }

        return when (backend.open(playerId, type)) {
            WorkstationOpenResult.Opened -> WorkstationResult.Opened
            WorkstationOpenResult.PlayerOffline -> WorkstationResult.PlayerOffline
            WorkstationOpenResult.Failed -> WorkstationResult.Failed
        }
    }

}
