package top.likoslupus.cellulosesz.common.world;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record SignTextMutation(
        SignTarget target,
        List<String> replacementLines
) {

    public SignTextMutation {
        requireNonNull(target, "target");
        replacementLines = List.copyOf(requireNonNull(replacementLines, "replacementLines"));
        if (replacementLines.size() != 4) {
            throw new IllegalArgumentException("sign must contain exactly four lines");
        }
    }

}
