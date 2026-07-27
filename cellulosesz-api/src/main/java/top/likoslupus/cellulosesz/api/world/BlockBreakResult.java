package top.likoslupus.cellulosesz.api.world;

import static java.util.Objects.requireNonNull;

public record BlockBreakResult(
        String blockId,
        boolean dropsEnabled
) {

    public BlockBreakResult {
        blockId = requireNonNull(blockId, "blockId");
    }

}
