package top.likoslupus.cellulosesz.utility.config

import kotlinx.serialization.Serializable

/** Root of the utility bounded context's configuration. */
@Serializable
public data class UtilitySettings(
    public val kits: KitSettings = KitSettings(),
    public val items: ItemUtilitySettings = ItemUtilitySettings(),
    public val itemEditing: ItemEditingSettings = ItemEditingSettings(),
    public val workstations: WorkstationSettings = WorkstationSettings(),
    public val give: GiveSettings = GiveSettings(),
    public val powertools: PowertoolSettings = PowertoolSettings(),
    public val inspection: InspectionSettings = InspectionSettings(),
)
