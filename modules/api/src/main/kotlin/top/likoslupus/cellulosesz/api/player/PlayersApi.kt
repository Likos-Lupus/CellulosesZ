package top.likoslupus.cellulosesz.api.player

/** Groups the Player domain: presence, nicknames, preferences and their fact events. */
public interface PlayersApi {

    public val presence: PlayerPresenceApi

    public val nicknames: PlayerNicknameApi

    public val preferences: PlayerPreferencesApi

    public val events: PlayerEvents

}
