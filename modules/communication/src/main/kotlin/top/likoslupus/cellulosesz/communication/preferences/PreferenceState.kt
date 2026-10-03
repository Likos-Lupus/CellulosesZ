package top.likoslupus.cellulosesz.communication.preferences

/** Server-thread confined load state of an online player's preferences. */
internal sealed interface PreferenceState {

    data object Loading : PreferenceState

    data class Ready(
        val value: MessagingPreferences,
    ) : PreferenceState

    data class Failed(
        val message: String,
    ) : PreferenceState

}
