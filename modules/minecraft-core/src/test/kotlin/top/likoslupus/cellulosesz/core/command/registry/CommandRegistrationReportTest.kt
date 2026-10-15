package top.likoslupus.cellulosesz.core.command.registry

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommandRegistrationReportTest {

    @Test
    fun `report projects installed literals, counts and conflicts`() {
        val report = CommandRegistrationReport(
            generation = 3,
            results = listOf(
                CommandRouteResult(
                    "cellulosesz",
                    CommandRouteKind.ADMIN_ROOT,
                    "cellulosesz",
                    CommandRouteStatus.INSTALLED,
                    null,
                ),
                CommandRouteResult(
                    "home",
                    CommandRouteKind.CANONICAL,
                    "home",
                    CommandRouteStatus.SKIPPED_ALREADY_PRESENT,
                    "cellulosesz:home",
                ),
                CommandRouteResult(
                    "cellulosesz:home",
                    CommandRouteKind.NAMESPACED,
                    "home",
                    CommandRouteStatus.INSTALLED,
                    null,
                ),
            ),
        )

        assertEquals(
            setOf("cellulosesz", "cellulosesz:home"),
            report.installedLiterals
        )
        assertEquals(2, report.installedCount)
        assertEquals(1, report.skippedCount)
        assertEquals("home", report.conflicts.single().literal)
        assertFalse(report.owns("home"))
        assertTrue(report.owns("cellulosesz:home"))
    }

}
