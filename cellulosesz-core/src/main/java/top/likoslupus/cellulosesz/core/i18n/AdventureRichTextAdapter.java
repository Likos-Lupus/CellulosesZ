package top.likoslupus.cellulosesz.core.i18n;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import top.likoslupus.cellulosesz.api.text.RichText;
import top.likoslupus.cellulosesz.api.text.TextStyle;

import java.util.ArrayList;

final class AdventureRichTextAdapter {

    private AdventureRichTextAdapter() {
    }

    static Component toComponent(RichText source) {
        var result = Component.empty();
        for (var segment : source.segments()) {
            result = result.append(
                    Component.text(segment.text())
                            .style(toAdventure(segment.style()))
            );
        }
        return result;
    }

    private static Style toAdventure(TextStyle source) {
        var builder = Style.style()
                .decoration(TextDecoration.BOLD, TextDecoration.State.byBoolean(source.bold()))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.byBoolean(source.italic()))
                .decoration(TextDecoration.UNDERLINED, TextDecoration.State.byBoolean(source.underlined()))
                .decoration(TextDecoration.STRIKETHROUGH, TextDecoration.State.byBoolean(source.strikethrough()))
                .decoration(TextDecoration.OBFUSCATED, TextDecoration.State.byBoolean(source.obfuscated()));
        if (!source.color().isBlank()) {
            var color = TextColor.fromHexString(source.color());
            if (color != null) builder.color(color);
        }
        return builder.build();
    }

    static RichText fromComponent(Component source) {
        var segments = new ArrayList<RichText.Segment>();
        append(source, TextStyle.EMPTY, segments);
        return new RichText(segments);
    }

    private static void append(
            Component component,
            TextStyle inherited,
            ArrayList<RichText.Segment> destination
    ) {
        var style = merge(inherited, component.style());
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            destination.add(new RichText.Segment(text.content(), style));
        }
        component.children().forEach(child -> append(child, style, destination));
    }

    private static TextStyle merge(TextStyle inherited, Style source) {
        var color = source.color() == null
                ? inherited.color()
                : "#%06X".formatted(source.color().value());
        return new TextStyle(
                color,
                decoration(source, TextDecoration.BOLD, inherited.bold()),
                decoration(source, TextDecoration.ITALIC, inherited.italic()),
                decoration(source, TextDecoration.UNDERLINED, inherited.underlined()),
                decoration(source, TextDecoration.STRIKETHROUGH, inherited.strikethrough()),
                decoration(source, TextDecoration.OBFUSCATED, inherited.obfuscated())
        );
    }

    private static boolean decoration(
            Style style,
            TextDecoration decoration,
            boolean inherited
    ) {
        return switch (style.decoration(decoration)) {
            case TRUE -> true;
            case FALSE -> false;
            case NOT_SET -> inherited;
        };
    }

}
