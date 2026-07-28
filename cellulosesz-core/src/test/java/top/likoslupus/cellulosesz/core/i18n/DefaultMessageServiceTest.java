package top.likoslupus.cellulosesz.core.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class DefaultMessageServiceTest {

    @TempDir
    Path directory;

    @Test
    void untrustedPlaceholderIsInsertedLiterally() {
        var messages = service();

        var rendered = messages.renderInline(
                "en_us",
                "<red><player>",
                Map.of("player", "<bold>Alice</bold>")
        );

        assertEquals("<bold>Alice</bold>", rendered.plainText());
        assertFalse(rendered.segments().stream().anyMatch(segment -> segment.style().bold()));
    }

    @Test
    void legacyColorsAreConvertedBeforeRendering() {
        var messages = service();

        var rendered = messages.renderInline("en_us", "&cFailure", Map.of());

        assertEquals("Failure", rendered.plainText());
        assertEquals("#FF5555", rendered.segments().getFirst().style().color());
    }

    @Test
    void unknownTagsRemainLiteral() {
        var messages = service();

        assertEquals(
                "<future_tag>value",
                messages.renderInline("en_us", "<future_tag>value", Map.of()).plainText()
        );
    }

    @Test
    void failedCandidateReloadDoesNotReplaceActiveSnapshot() throws IOException {
        var messages = service();
        messages.reload();
        var before = messages.message("commands.economy.pay-offline-denied");

        var chinese = directory.resolve("zh_cn.yml");
        var invalid = Files.readString(chinese)
                .replaceFirst("<permission>", "<different_placeholder>");
        Files.writeString(chinese, invalid);

        assertThrows(IllegalStateException.class, () -> messages.prepareReload("zh_cn", "en_us"));
        assertEquals(before, messages.message("commands.economy.pay-offline-denied"));
    }

    private DefaultMessageService service() {
        var messages = new DefaultMessageService(directory, new NoopLogger());
        messages.locales("en_us", "en_us");
        messages.theme("#55FF55", "#FFFF55", true);
        return messages;
    }

    private static final class NoopLogger implements CellulosesZLogger {

        @Override
        public void warn(String message) {
        }

        @Override
        public void error(String message) {
        }

        @Override
        public void error(String message, Throwable throwable) {
        }

        @Override
        public void info(String message) {
        }

    }

}
