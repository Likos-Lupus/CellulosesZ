package top.likoslupus.cellulosesz.modules.text.application;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.text.TextService;
import top.likoslupus.cellulosesz.core.i18n.GeneratedMessageKeys;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class DefaultTextCommandServiceTest {

    private final DefaultTextCommandService service = new DefaultTextCommandService(new FixedTextService());

    @Test
    void defaultAndTypedSecondPageSelectExpectedLines() {
        var first = service.info(1);
        var second = service.info(2);

        assertTrue(first.success());
        assertEquals(3, first.messages().size());
        assertEquals("line-1", first.messages().get(1).placeholders().get("line"));
        assertEquals("line-2", first.messages().get(2).placeholders().get("line"));
        assertTrue(second.success());
        assertEquals("line-3", second.messages().get(1).placeholders().get("line"));
        assertEquals(2, second.messages().get(0).placeholders().get("page"));
        assertEquals(2, second.messages().get(0).placeholders().get("pages"));
    }

    @Test
    void nonPositiveAndOutOfRangePagesAreDistinctFailures() {
        var zero = service.info(0);
        var tooHigh = service.info(3);

        assertFalse(zero.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_COMMON_INVALID_PAGE,
                zero.messages().getFirst().key()
        );
        assertFalse(tooHigh.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                tooHigh.messages().getFirst().key()
        );
    }

    @Test
    void customSectionAndMissingSectionRemainDistinctFromPageFailure() {
        var section = service.custom("guide", 2);
        var missing = service.custom("missing", 1);
        var page = service.custom("guide", 9);

        assertTrue(section.success());
        assertEquals("line-3", section.messages().get(1).placeholders().get("line"));
        assertFalse(missing.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_TEXT_CUSTOM_MISSING,
                missing.messages().getFirst().key()
        );
        assertFalse(page.success());
        assertEquals(
                GeneratedMessageKeys.COMMANDS_COMMON_PAGE_OUT_OF_RANGE,
                page.messages().getFirst().key()
        );
    }

    @Test
    void customSuggestionsComeFromTextService() {
        assertEquals(Set.of("guide", "faq"), service.customNames());
    }

    @NullMarked
    private static final class FixedTextService implements TextService {

        @Override
        public List<String> info() {
            return List.of("line-1", "line-2", "line-3");
        }

        @Override
        public List<String> motd() {
            return List.of("motd");
        }

        @Override
        public List<String> rules() {
            return List.of("rule");
        }

        @Override
        public List<String> custom(String name) {
            return name.equalsIgnoreCase("guide")
                    ? List.of("line-1", "line-2", "line-3")
                    : List.of();
        }

        @Override
        public Set<String> customNames() {
            return Set.of("guide", "faq");
        }

        @Override
        public int pageSize() {
            return 2;
        }

    }

}
