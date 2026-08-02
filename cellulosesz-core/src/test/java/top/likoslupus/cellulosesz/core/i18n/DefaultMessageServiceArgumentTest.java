package top.likoslupus.cellulosesz.core.i18n;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.LocalizedMessage;
import top.likoslupus.cellulosesz.api.text.MessageArguments;
import top.likoslupus.cellulosesz.api.text.RichText;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class DefaultMessageServiceArgumentTest {

    @Test
    void rendersEveryTypedPlaceholderVariant(@TempDir Path directory) {
        var service = new DefaultMessageService(directory, new NoopLogger());
        service.reload();
        var uuid = UUID.fromString("00000000-0000-0000-0000-000000000777");

        var rendered = service.renderInline(
                "en_us",
                "<text>|<number>|<boolean>|<uuid>|<rich>|<nested>",
                MessageArguments.builder()
                        .put("text", "value")
                        .put("number", new BigDecimal("12.50"))
                        .put("boolean", true)
                        .put("uuid", uuid)
                        .put("rich", RichText.plain("rich"))
                        .put("nested", LocalizedMessage.of("common.console"))
                        .build()
        );

        assertEquals(
                "value|12.50|true|" + uuid + "|rich|Console",
                rendered.plainText()
        );
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
