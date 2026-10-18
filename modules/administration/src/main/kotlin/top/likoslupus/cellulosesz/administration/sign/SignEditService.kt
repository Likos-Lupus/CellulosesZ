package top.likoslupus.cellulosesz.administration.sign

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText
import java.util.*

/**
 * Server-thread confined sign editing: reads and rewrites the four lines of a `SignBlockEntity`
 * front (or back) text and keeps a session-only clipboard per player. Click/hover events are always
 * dropped because the stored text is rebuilt from plain text plus optional legacy colour codes.
 */
internal class SignEditService {

    private val clipboards = HashMap<UUID, List<String>>()

    fun signAt(level: ServerLevel, pos: BlockPos): SignBlockEntity? =
        level.getBlockEntity(pos) as? SignBlockEntity

    fun lines(sign: SignBlockEntity, back: Boolean): List<String> =
        (0 until SignText.LINES).map { index ->
            sign.getText(back).getMessage(index, false).string
        }

    /** Replaces one line. Returns the stored plain text. */
    fun setLine(
        sign: SignBlockEntity,
        back: Boolean,
        line: Int,
        rawText: String,
        allowColors: Boolean,
    ): String {
        val plain = LegacyText.plain(rawText)
        val component = component(allowColors, rawText)
        val text = sign.getText(back)
        text.setMessage(line, component)
        sign.setText(text, back)
        markDirty(sign)
        return plain
    }

    fun clearLine(
        sign: SignBlockEntity,
        back: Boolean,
        line: Int,
    ) {
        val text = sign.getText(back)
        text.setMessage(line, Component.empty())
        sign.setText(text, back)
        markDirty(sign)
    }

    fun clearAll(sign: SignBlockEntity, back: Boolean) {
        val text = sign.getText(back)
        (0 until SignText.LINES).forEach { index ->
            text.setMessage(index, Component.empty())
        }
        sign.setText(text, back)
        markDirty(sign)
    }

    fun copy(playerId: UUID, lines: List<String>) {
        clipboards[playerId] = lines
    }

    fun clipboard(playerId: UUID): List<String>? =
        clipboards[playerId]

    fun paste(
        sign: SignBlockEntity,
        back: Boolean,
        lines: List<String>,
        allowColors: Boolean
    ) {
        val text = sign.getText(back)
        lines.take(SignText.LINES).forEachIndexed { index, raw ->
            text.setMessage(index, component(allowColors, raw))
        }
        sign.setText(text, back)
        markDirty(sign)
    }

    fun clear(playerId: UUID) {
        clipboards.remove(playerId)
    }

    fun clearAll() {
        clipboards.clear()
    }

    private fun markDirty(sign: SignBlockEntity) {
        sign.setChanged()
        val level = sign.level as? ServerLevel ?: return
        val state = level.getBlockState(sign.blockPos)
        level.sendBlockUpdated(
            sign.blockPos,
            state,
            state,
            3
        )
    }

    private fun component(allowColors: Boolean, raw: String): Component {
        val root: MutableComponent = Component.empty()
        LegacyText.parse(raw, allowColors).forEach { segment ->
            val literal = Component.literal(segment.text)
            val code = segment.code
            if (code != null) {
                ChatFormatting.getByCode(code)?.let { literal.withStyle(it) }
            }
            root.append(literal)
        }
        return root
    }

}
