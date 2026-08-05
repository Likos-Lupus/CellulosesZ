package top.likoslupus.cellulosesz.core.i18n;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.MessageArguments;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class MessageRendererTest {

    @Test
    void render_withOrderedArguments_usesIndexes() {
        var renderer = renderer(Map.of("message", "{0} -> {1}"));

        var result = renderer.render(
                "en_us",
                "message",
                MessageArguments.builder().add("first").add("second").build()
        );

        assertEquals("first -> second", result.plainText());
    }

    private static DefaultMessageService renderer(Map<String, String> catalog) {
        return renderer(catalog, catalog);
    }

    private static DefaultMessageService renderer(
            Map<String, String> english,
            Map<String, String> chinese
    ) {
        var renderer = new DefaultMessageService(new SilentLogger());
        renderer.replaceCatalogs(Map.of("en_us", english, "zh_cn", chinese));
        return renderer;
    }

    @Test
    void render_withRepeatedPosition_reusesSameArgument() {
        var renderer = renderer(Map.of("message", "{0}/{0}"));

        var result = renderer.render(
                "en_us",
                "message",
                MessageArguments.builder().add("same").build()
        );

        assertEquals("same/same", result.plainText());
    }

    @Test
    void render_afterTranslationReordersArguments_preservesMeaning() {
        var renderer = renderer(
                Map.of("message", "{0} sent {1} to {2}."),
                Map.of("message", "{0} 向 {2} 发送了 {1}。")
        );
        var arguments = MessageArguments.builder()
                .add("Alex")
                .add("a letter")
                .add("Steve")
                .build();

        assertEquals(
                "Alex 向 Steve 发送了 a letter。",
                renderer.render("zh_cn", "message", arguments).plainText()
        );
    }

    @Test
    void render_withMarkupLikeArgument_treatsArgumentAsLiteral() {
        var renderer = renderer(Map.of("message", "<primary>{0}"));
        var unsafe = "<red>literal\\$ {0}";

        var result = renderer.render(
                "en_us",
                "message",
                MessageArguments.builder().add(unsafe).build()
        );

        assertEquals(unsafe, result.plainText());
    }

    private static final class SilentLogger implements CellulosesZLogger {

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
