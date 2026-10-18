# /book

Edit or convert the held book.

## Parameters

- No argument converts between a written and a writable book.
- `author <name>`: set the author of a written book.
- `title <title>`: set the title of a written book.

## Behavior

Operates on the book in your main hand. Author and title require a written book; conversion
preserves the pages.

## Examples

- `/book`
- `/book author Notch`
- `/book title My Story`

## Failures

- Nothing is held or the held item is not a book.
- Author/title is used on a writable book.

## Related commands

- `/itemname`, `/itemlore`
