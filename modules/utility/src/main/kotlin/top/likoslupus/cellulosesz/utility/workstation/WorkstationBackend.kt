package top.likoslupus.cellulosesz.utility.workstation

import java.util.*

internal sealed interface WorkstationOpenResult {

    data object Opened : WorkstationOpenResult
    data object PlayerOffline : WorkstationOpenResult
    data object Failed : WorkstationOpenResult

}

/**
 * The single seam through which utility opens a menu. Implementations are server-thread confined;
 * only vanilla menu types are used because the project has no client-side screen code.
 */
internal interface WorkstationBackend {

    fun open(playerId: UUID, type: WorkstationType): WorkstationOpenResult

}
