package top.likoslupus.cellulosesz.common.text;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import top.likoslupus.cellulosesz.api.logging.CellulosesZLogger;
import top.likoslupus.cellulosesz.api.text.RichText;

import static java.util.Objects.requireNonNull;

public final class MinecraftTextAdapter {

    private MinecraftTextAdapter() {
    }

    public static Component toComponent(RichText text, CellulosesZLogger logger) {
        requireNonNull(text, "text");
        requireNonNull(logger, "logger");

        var root = Component.empty();
        text.segments().forEach(segment -> {
            var part = Component.literal(segment.text());
            var source = segment.style();

            part.withStyle(style -> {
                var updated = style
                        .withBold(source.bold())
                        .withItalic(source.italic())
                        .withUnderlined(source.underlined())
                        .withStrikethrough(source.strikethrough())
                        .withObfuscated(source.obfuscated());
                var color = source.color();

                if (!color.isBlank()) {
                    if (color.matches("#[0-9a-fA-F]{6}")) {
                        updated = updated.withColor(
                                TextColor.fromRgb(
                                        Integer.parseInt(
                                                color.substring(1),
                                                16
                                        )
                                )
                        );
                    } else {
                        logger.warn("Ignoring invalid RichText color: " + color);
                    }
                }

                return updated;
            });

            root.append(part);
        });

        return root;
    }

}
