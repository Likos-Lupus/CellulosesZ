package top.likoslupus.cellulosesz.api.text

@JvmRecord
public data class TextStyle(
    public val color: String,
    public val bold: Boolean,
    public val italic: Boolean,
    public val underlined: Boolean,
    public val strikethrough: Boolean,
    public val obfuscated: Boolean
) {

    public fun withColor(color: String): TextStyle = copy(color = color)

    public fun withBold(value: Boolean): TextStyle = copy(bold = value)

    public fun withItalic(value: Boolean): TextStyle = copy(italic = value)

    public fun withUnderlined(value: Boolean): TextStyle = copy(underlined = value)

    public fun withStrikethrough(value: Boolean): TextStyle = copy(strikethrough = value)

    public fun withObfuscated(value: Boolean): TextStyle = copy(obfuscated = value)

    public companion object {

        @JvmField
        public val EMPTY: TextStyle = TextStyle(
            color = "",
            bold = false,
            italic = false,
            underlined = false,
            strikethrough = false,
            obfuscated = false
        )

    }

}
