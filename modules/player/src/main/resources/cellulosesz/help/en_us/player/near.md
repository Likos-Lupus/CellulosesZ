# /near

List players close to you.

## Usage

- `/near`
- `/near <radius>`

## Limits

Only players in the same dimension are listed, and the radius is capped by the server. Spectators
may be hidden. Asking for a radius above the server maximum is rejected unless you hold the
`near.bypass_radius` capability.

## Related commands

- `/ping`, `/seen`
