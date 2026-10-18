package top.likoslupus.cellulosesz.administration.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Root of the administration bounded context's configuration. */
@Serializable
public data class AdministrationSettings(
    public val moderation: ModerationSettings = ModerationSettings(),
    public val jail: JailSettings = JailSettings(),
    public val playerAdjustments: PlayerAdjustmentSettings = PlayerAdjustmentSettings(),
    public val entityTools: EntityToolsSettings = EntityToolsSettings(),
    public val worldTools: WorldToolsSettings = WorldToolsSettings(),
    @SerialName("fun")
    public val amusement: FunSettings = FunSettings(),
    public val signEditing: SignEditingSettings = SignEditingSettings(),
    public val vanish: VanishSettings = VanishSettings(),
)
