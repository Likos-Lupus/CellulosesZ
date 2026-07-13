package top.likoslupus.cellulosesz.api.teleport;

public final class CellLocation {

    public String world = "minecraft:overworld";
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;

    public CellLocation() {
    }

    public CellLocation(
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public CellLocation withWorld(String world) {
        return new CellLocation(world, x, y, z, yaw, pitch);
    }

    public CellLocation withPosition(
            double x,
            double y,
            double z
    ) {
        return new CellLocation(world, x, y, z, yaw, pitch);
    }

    public String compact() {
        return "%s %.2f %.2f %.2f".formatted(world, x, y, z);
    }

}
