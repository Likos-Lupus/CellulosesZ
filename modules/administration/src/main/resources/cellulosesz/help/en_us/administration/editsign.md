# /editsign

Edit the text of the sign you are looking at.

## Behavior

Edits only text: every click and hover event is stripped from the input. The side you are facing is
edited; the back requires `[administration.signEditing].allowBack`. Colour codes are applied only
with the colour permission. `set` writes one line, `clear` empties one line or the whole sign,
`copy` stores the sign text in a session clipboard, and `paste` writes that clipboard back. The
clipboard is cleared when you quit.

## Parameters

- `set`: write a line.
- `clear`: empty a line, or the whole sign when no line is given.
- `copy`: copy the sign text.
- `paste`: paste the copied text.
- `line`: the line number, 1 to 4.
- `text`: the text to write.

## Example

- `/editsign set 1 Welcome`
- `/editsign clear 2`
- `/editsign copy`
- `/editsign paste`

## Failures

- You are not looking at a sign.
- Sign editing is disabled, the back is disabled, or you are too far away.
- The text is longer than the configured line limit.

## Related commands

- `/break`, `/gc`
