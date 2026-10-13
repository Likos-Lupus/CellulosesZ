# /warpinfo

Show the saved details of a warp.

## Behavior

Always shows the warp's dimension. Senders with the warp coordinate permission also see the exact
position, who saved the warp and when it was created and last updated. Without that permission a
player sees only the name and the dimension, so exact locations stay private.

## Example

- `/warpinfo market`

## Failures

- The warp doesn't exist.
- Missing permission for exact coordinates (the command still shows the dimension).

## Related

- `/warp`, `/warps`, `/setwarp`, `/delwarp`
