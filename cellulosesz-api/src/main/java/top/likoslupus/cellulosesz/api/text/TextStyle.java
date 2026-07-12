package top.likoslupus.cellulosesz.api.text;

public record TextStyle(
        String color,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated
) {

    public static final TextStyle EMPTY = new TextStyle(
            "",
            false,
            false,
            false,
            false,
            false
    );

    public TextStyle withColor(String color) {
        return new TextStyle(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public TextStyle withBold(boolean value) {
        return new TextStyle(color, value, italic, underlined, strikethrough, obfuscated);
    }

    public TextStyle withItalic(boolean value) {
        return new TextStyle(color, bold, value, underlined, strikethrough, obfuscated);
    }

    public TextStyle withUnderlined(boolean value) {
        return new TextStyle(color, bold, italic, value, strikethrough, obfuscated);
    }

    public TextStyle withStrikethrough(boolean value) {
        return new TextStyle(color, bold, italic, underlined, value, obfuscated);
    }

    public TextStyle withObfuscated(boolean value) {
        return new TextStyle(color, bold, italic, underlined, strikethrough, value);
    }

}
