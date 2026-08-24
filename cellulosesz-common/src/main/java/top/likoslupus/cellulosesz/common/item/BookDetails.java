package top.likoslupus.cellulosesz.common.item;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public record BookDetails(
        boolean writable,
        boolean written,
        Optional<String> title,
        Optional<String> author,
        int pageCount
) {

    public BookDetails {
        requireNonNull(title, "title");
        requireNonNull(author, "author");
        if (pageCount < 0) {
            throw new IllegalArgumentException("pageCount must not be negative");
        }
        if (writable == written) {
            throw new IllegalArgumentException("book must be writable or written");
        }
    }

}
