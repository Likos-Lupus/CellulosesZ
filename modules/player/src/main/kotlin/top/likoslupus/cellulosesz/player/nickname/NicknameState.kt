package top.likoslupus.cellulosesz.player.nickname

import net.minecraft.network.chat.Component
import top.likoslupus.cellulosesz.player.nickname.NicknameState.applyToVanillaDisplay
import java.util.*

/**
 * Process-wide snapshot of online players' nickname display components, read by
 * [top.likoslupus.cellulosesz.player.mixin.PlayerDisplayNameMixin] on the render path and written
 * only by [NicknameService]. A tiny static bridge is required because the mixin has no reference to
 * the composition graph. Only online players ever have an entry, so lookups stay allocation-free.
 *
 * The global vanilla `Player.getDisplayName` override is opt-in: [applyToVanillaDisplay] mirrors
 * `players.nicknames.applyToVanillaDisplay` and defaults off, so by default CellulosesZ only renders
 * nicknames at its own controlled sites.
 */
internal object NicknameState {

    @Volatile private var displayNames: Map<UUID, Component> = emptyMap()

    @Volatile private var applyToVanillaDisplay: Boolean = false

    fun update(playerId: UUID, displayName: Component) {
        displayNames = displayNames + (playerId to displayName)
    }

    fun clear(playerId: UUID) {
        if (displayNames.containsKey(playerId)) {
            displayNames = displayNames - playerId
        }
    }

    fun clearAll() {
        displayNames = emptyMap()
    }

    fun setApplyToVanillaDisplay(enabled: Boolean) {
        applyToVanillaDisplay = enabled
    }

    fun displayName(playerId: UUID): Component? =
        displayNames[playerId]

    /** The nickname the vanilla render path should use, or null when the override is off. */
    fun displayNameForVanilla(playerId: UUID): Component? =
        when {
            !applyToVanillaDisplay -> null
            else -> displayNames[playerId]
        }

}
