package top.likoslupus.cellulosesz.player.config

import kotlinx.serialization.Serializable

/** Player presence, session, nickname and nearby-query settings. */
@Serializable
public data class PlayerSettings(
    public val afk: AfkSettings = AfkSettings(),
    public val sessions: SessionSettings = SessionSettings(),
    public val nicknames: NicknameSettings = NicknameSettings(),
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
)

@Serializable
public data class NearbySettings(
    public val defaultRadius: Int = 100,
    public val maximumRadius: Int = 256,
    public val maximumResults: Int = 20,
    public val hideSpectators: Boolean = true,
)
