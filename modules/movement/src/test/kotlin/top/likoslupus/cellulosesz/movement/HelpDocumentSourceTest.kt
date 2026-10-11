package top.likoslupus.cellulosesz.movement

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import top.likoslupus.cellulosesz.core.command.dsl.CommandDocumentId
import top.likoslupus.cellulosesz.core.text.document.ClasspathCommandDocumentSource
import top.likoslupus.cellulosesz.core.text.i18n.LanguageId

/**
 * Guards that help documents are reachable through the owning module's class loader rather than the
 * reader's: documents are bundled per module and, at runtime, modules may be separate jars.
 */
class HelpDocumentSourceTest {

    @Test
    fun `movement documents load through the movement class loader`() {
        val source = ClasspathCommandDocumentSource(MovementFeature::class.java)
        assertNotNull(source.read(CommandDocumentId.of("movement/home"), LanguageId.EN_US))
        assertNotNull(source.read(CommandDocumentId.of("movement/tp"), LanguageId.ZH_CN))
    }

}
