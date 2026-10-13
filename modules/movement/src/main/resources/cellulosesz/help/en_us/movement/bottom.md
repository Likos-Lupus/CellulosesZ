# /bottom

Teleport to the lowest safe spot below you.

## Behavior

Finds the first safe standing space above the lowest block in your current column. If the area isn't
loaded you are not moved.

## Example

- `/bottom`

## Failures

- The terrain feature is disabled.
- The area isn't loaded.
- No safe spot was found.
- The teleport was canceled or blocked.

## Related

- `/top`, `/jump`
