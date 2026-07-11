package top.likoslupus.cellulosesz.api.item;

/**
 * Explicit raw command/SNBT component value.
 */
public record RawItemComponent(
        String value
) {

    public RawItemComponent {
        value = value.trim();
    }

}
