package top.likoslupus.cellulosesz.api.text

@JvmRecord
public data class RichText(
    public val segments: List<Segment>
) {

    public fun append(other: RichText): RichText {
        if (segments.isEmpty()) {
            return other
        }

        if (other.segments.isEmpty()) {
            return this
        }

        val merged = ArrayList<Segment>(segments.size + other.segments.size)
        merged.addAll(segments)
        merged.addAll(other.segments)
        return RichText(merged)
    }

    public fun plainText(): String =
        segments.joinToString("") { it.text }

    @JvmRecord
    public data class Segment(
        public val text: String,
        public val style: TextStyle
    )

    public companion object {

        @JvmStatic
        public fun plain(value: String): RichText =
            if (value.isEmpty()) {
                empty()
            } else {
                RichText(listOf(Segment(value, TextStyle.EMPTY)))
            }

        @JvmStatic
        public fun empty(): RichText = RichText(emptyList())

    }

}
