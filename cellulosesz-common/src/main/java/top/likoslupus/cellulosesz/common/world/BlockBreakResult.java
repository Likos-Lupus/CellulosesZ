package top.likoslupus.cellulosesz.common.world;

import static java.util.Objects.requireNonNull;

public record BlockBreakResult(
        String blockId,
        boolean dropsEnabled
) {

    public BlockBreakResult {
        blockId = requireNonNull(blockId, "blockId");
    }

}
