package top.likoslupus.cellulosesz.api.text;

import java.util.ArrayList;
import java.util.List;

public record RichText(List<Segment> segments) {

    public RichText {
        segments = List.copyOf(segments);
    }

    public static RichText plain(String value) {
        return value.isEmpty()
                ? empty()
                : new RichText(List.of(new Segment(value, TextStyle.EMPTY)));
    }

    public static RichText empty() {
        return new RichText(List.of());
    }

    public RichText append(RichText other) {
        if (segments.isEmpty()) return other;
        if (other.segments.isEmpty()) return this;

        var merged = new ArrayList<Segment>(segments.size() + other.segments.size());
        merged.addAll(segments);
        merged.addAll(other.segments);
        return new RichText(merged);
    }

    public String plainText() {
        return segments.stream()
                .map(Segment::text)
                .reduce("", String::concat);
    }

    public record Segment(
            String text,
            TextStyle style
    ) {

    }

}
