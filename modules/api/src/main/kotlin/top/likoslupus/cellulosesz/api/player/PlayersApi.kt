package top.likoslupus.cellulosesz.api.player

/** Groups the Player domain: presence, nicknames and their fact events. */
public interface PlayersApi {

    public val presence: PlayerPresenceApi

    public val nicknames: PlayerNicknameApi

    public val events: PlayerEvents

}
