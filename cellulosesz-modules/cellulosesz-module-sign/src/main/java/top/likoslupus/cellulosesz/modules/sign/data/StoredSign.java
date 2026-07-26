package top.likoslupus.cellulosesz.modules.sign.data;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public final class StoredSign {

    public String world = "";
    public int x;
    public int y;
    public int z;
    public boolean front;
    public String type = "";
    public List<String> lines = List.of();
    public UUID owner = new UUID(0L, 0L);

    public StoredSign() {
    }

    public StoredSign(
            String world,
            int x,
            int y,
            int z,
            boolean front,
            String type,
            List<String> lines,
            UUID owner
    ) {
        this.world = requireText(world, "world");
        this.x = x;
        this.y = y;
        this.z = z;
        this.front = front;
        this.type = requireText(type, "type");
        this.lines = List.copyOf(requireNonNull(lines, "lines"));
        if (this.lines.size() != 4) {
            throw new IllegalArgumentException("Sign lines must contain exactly four entries");
        }
        this.owner = requireNonNull(owner, "owner");
    }

    private static String requireText(String value, String field) {
        var result = requireNonNull(value, field).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return result;
    }

    public void validate() {
        world = requireText(world, "world");
        type = requireText(type, "type");
        lines = List.copyOf(requireNonNull(lines, "lines"));
        if (lines.size() != 4) {
            throw new IllegalArgumentException("Sign lines must contain exactly four entries");
        }
        requireNonNull(owner, "owner");
    }

    public StoredSign copy() {
        return new StoredSign(world, x, y, z, front, type, lines, owner);
    }

}
