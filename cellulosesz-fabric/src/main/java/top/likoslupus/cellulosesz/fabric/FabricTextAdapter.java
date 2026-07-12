package top.likoslupus.cellulosesz.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import top.likoslupus.cellulosesz.api.text.RichText;

public final class FabricTextAdapter {

    private FabricTextAdapter() {
    }

    public static Component toComponent(RichText text) {
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
                if (!source.color().isBlank()) {
                    try {
                        updated = updated.withColor(
                                TextColor.fromRgb(
                                        Integer.parseInt(
                                                source.color().substring(1), 16
                                        )
                                )
                        );
                    } catch (RuntimeException _) {
                    }
                }
                return updated;
            });
            root.append(part);
        });
        return root;
    }

}
