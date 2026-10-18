package top.likoslupus.cellulosesz.player.config

import kotlinx.serialization.Serializable

/** Player presence, session, nickname, list and nearby-query settings. */
@Serializable
public data class PlayerSettings(
    public val afk: AfkSettings = AfkSettings(),
    public val sessions: SessionSettings = SessionSettings(),
    public val nicknames: NicknameSettings = NicknameSettings(),
    public val list: PlayerListSettings = PlayerListSettings(),
    public val nearby: NearbySettings = NearbySettings(),
)

@Serializable
public data class AfkSettings(
    public val enabled: Boolean = true,
    public val autoAfterSeconds: Int = 300,
    public val checkIntervalTicks: Int = 20,
    public val movementThreshold: Double = 0.15,
    public val rotationThresholdDegrees: Double = 3.0,
    public val manualClearOnActivity: Boolean = false,
    public val announceStateChanges: Boolean = false,
    /** Kick an AFK player after this many seconds; 0 disables the kick. */
    public val kickAfterSeconds: Int = 0,
    /** Cancel a frozen player's movement packets until they leave AFK. */
    public val freezeWhileAfk: Boolean = false,
    /** Prevent an AFK player from picking items up. */
    public val disableItemPickup: Boolean = false,
    /** Leave AFK when the player interacts with the world. */
    public val cancelOnInteract: Boolean = false,
    /** Leave AFK when the player sends a chat message. */
    public val cancelOnChat: Boolean = false,
    /** Leave AFK when the player casts a fishing hook. */
    public val cancelOnFish: Boolean = false,
    /** Exclude AFK players from the sleep-vote requirement. */
    public val sleepIgnoresAfk: Boolean = false,
    /** Marker appended to a player's name in `/list` while they are AFK. */
    public val listName: String = "[AFK]",
)

@Serializable
public data class SessionSettings(
    public val enabled: Boolean = true,
    public val heartbeatIntervalSeconds: Int = 60,
    public val maxRetryBufferSize: Int = 1024,
)

@Serializable
public data class NicknameSettings(
    public val enabled: Boolean = true,
    public val minimumVisibleLength: Int = 2,
    public val maximumVisibleLength: Int = 24,
    public val allowSpaces: Boolean = true,
    public val applyToVanillaDisplay: Boolean = false,
    /** Allow legacy `&` colour/format codes when the player also holds `NICK_COLORS`. */
    public val allowColors: Boolean = false,
    /** Optional full-match regex for the visible (stripped) nickname; empty means no constraint. */
    public val allowedPattern: String = "",
    /** Case-insensitive substrings the visible nickname must not contain. */
    public val blacklist: List<String> = emptyList(),
    /** Prefix shown before the nickname at every display site. */
    public val prefix: String = "",
    /** Clear a nickname when the owner's account name changes. */
    public val resetOnNameChange: Boolean = false,
)

/**
 * `/list` grouping. Every online player is assigned to the first group whose [PlayerListGroup.permission]
 * they hold, falling back to [defaultGroup]; the group order is the order of [groups].
 */
@Serializable
public data class PlayerListSettings(
    public val groups: List<PlayerListGroup> = emptyList(),
    public val defaultGroup: PlayerListGroup = PlayerListGroup(),
)

@Serializable
public data class PlayerListGroup(
    public val id: String = "default",
    public val permission: String = "",
    public val label: String = "Default",
)

@Serializable
public data class NearbySettings(
    public val defaultRadius: Int = 100,
    public val maximumRadius: Int = 256,
    public val maximumResults: Int = 20,
    public val hideSpectators: Boolean = true,
)
