package top.likoslupus.cellulosesz.api.teleport;

import static top.likoslupus.cellulosesz.api.validation.NumericChecks.requireFinite;
import static top.likoslupus.cellulosesz.api.validation.TextChecks.requireNonBlank;

import static java.util.Objects.requireNonNull;

/** Immutable, platform-neutral world position. */
public record CellLocation(
        String world,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
) {

    public CellLocation {
        world = requireNonBlank(requireNonNull(world, "world").trim(), "world");
        requireFinite(x, "x");
        requireFinite(y, "y");
        requireFinite(z, "z");
        requireFinite(yaw, "yaw");
        requireFinite(pitch, "pitch");
    }

    public CellLocation withWorld(String world) {
        return new CellLocation(world, x, y, z, yaw, pitch);
    }

    public CellLocation withPosition(double x, double y, double z) {
        return new CellLocation(world, x, y, z, yaw, pitch);
    }

    public String compact() {
        return "%s %.2f %.2f %.2f".formatted(world, x, y, z);
    }

}
