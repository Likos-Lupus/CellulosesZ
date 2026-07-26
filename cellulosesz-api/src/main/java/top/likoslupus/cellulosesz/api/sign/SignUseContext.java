package top.likoslupus.cellulosesz.api.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;
import top.likoslupus.cellulosesz.api.teleport.CellLocation;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record SignUseContext(
        CellPlayer player,
        CellLocation location,
        boolean front,
        List<String> lines,
        boolean sneaking
) {

    public SignUseContext {
        requireNonNull(player, "player");
        requireNonNull(location, "location");
        lines = List.copyOf(requireNonNull(lines, "lines"));
    }

    public String line(int index) {
        if (index < 0 || index >= lines.size()) return "";
        return lines.get(index).trim();
    }

}
