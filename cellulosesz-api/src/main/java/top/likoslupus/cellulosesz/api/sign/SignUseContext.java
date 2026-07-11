package top.likoslupus.cellulosesz.api.sign;

import top.likoslupus.cellulosesz.api.platform.CellPlayer;

import java.util.List;

public record SignUseContext(
        CellPlayer player,
        List<String> lines,
        boolean sneaking
) {

    public SignUseContext {
        lines = List.copyOf(lines);
    }

    public String line(int index) {
        if (index < 0 || index >= lines.size()) return "";
        return lines.get(index).trim();
    }

}
