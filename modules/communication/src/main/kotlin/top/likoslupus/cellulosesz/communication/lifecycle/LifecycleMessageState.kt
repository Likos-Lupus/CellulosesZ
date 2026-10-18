package top.likoslupus.cellulosesz.communication.lifecycle

import java.util.*

/** Which lifecycle edge an announcement belongs to. */
internal enum class LifecycleKind {

    JOIN,
    QUIT,

}

/**
 * A join/quit announcement resolved at hook time. [firstJoin] and [newUsername] are captured at
 * `placeNewPlayer` HEAD, before any join handler can enlarge the known-identity index.
 */
internal data class LifecyclePlayer(
    val playerId: UUID,
    val username: String,
    val displayName: String,
    val worldId: String,
    val firstJoin: Boolean,
    val newUsername: Boolean,
    val previousName: String?,
)

/**
 * The composition-root-installed policy for lifecycle announcements. It runs on the server thread;
 * it decides (and performs) the custom broadcast and reports whether the vanilla message must be
 * cancelled.
 */
internal interface LifecycleMessageController {

    fun prepareJoin(
        playerId: UUID,
        username: String,
        displayName: String,
        worldId: String,
    ): LifecyclePlayer

    fun prepareQuit(
        playerId: UUID,
        username: String,
        displayName: String,
        worldId: String,
    ): LifecyclePlayer

    /** Returns true when the vanilla join/quit broadcast must be cancelled. */
    fun handle(
        kind: LifecycleKind,
        player: LifecyclePlayer,
    ): Boolean

    companion object {

        /** Pass-through controller used before install/after uninstall: never alters vanilla. */
        val Disabled: LifecycleMessageController = object : LifecycleMessageController {

            override fun prepareJoin(
                playerId: UUID,
                username: String,
                displayName: String,
                worldId: String,
            ): LifecyclePlayer = LifecyclePlayer(
                playerId = playerId,
                username = username,
                displayName = displayName,
                worldId = worldId,
                firstJoin = false,
                newUsername = false,
                previousName = null,
            )

            override fun prepareQuit(
                playerId: UUID,
                username: String,
                displayName: String,
                worldId: String,
            ): LifecyclePlayer = prepareJoin(playerId, username, displayName, worldId)

            override fun handle(
                kind: LifecycleKind,
                player: LifecyclePlayer,
            ): Boolean = false

        }

    }

}

/**
 * Process-wide bridge between the server mixins and the communication feature. It lives outside the
 * `communication.mixin` package because Mixin forbids normal code from referencing classes declared
 * in that package. Only the server thread writes the pending player; the controller snapshot is
 * volatile so the mixin reads a fully constructed policy.
 */
internal object LifecycleMessageState {

    @Volatile private var controller: LifecycleMessageController =
        LifecycleMessageController.Disabled

    private var pending: LifecyclePlayer? = null

    fun install(controller: LifecycleMessageController) {
        this.controller = controller
    }

    fun uninstall() {
        controller = LifecycleMessageController.Disabled
        pending = null
    }

    fun prepareJoin(
        playerId: UUID,
        username: String,
        displayName: String,
        worldId: String,
    ) {
        pending = controller.prepareJoin(
            playerId,
            username,
            displayName,
            worldId
        )
    }

    fun prepareQuit(
        playerId: UUID,
        username: String,
        displayName: String,
        worldId: String,
    ) {
        pending = controller.prepareQuit(
            playerId,
            username,
            displayName,
            worldId
        )
    }

    fun clearPending() {
        pending = null
    }

    /** Consumes the pending player (if any) and lets the controller decide. Server thread only. */
    fun handle(kind: LifecycleKind): Boolean {
        val current = pending ?: return false
        pending = null
        return controller.handle(kind, current)
    }

}
