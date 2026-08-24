package top.likoslupus.cellulosesz.modules.teleport.domain;

import java.util.List;

import static java.util.Objects.requireNonNull;

public sealed interface TeleportRequestSelectionResult permits
        TeleportRequestSelectionResult.None,
        TeleportRequestSelectionResult.Selected,
        TeleportRequestSelectionResult.Ambiguous {

    record None() implements TeleportRequestSelectionResult {

    }

    record Selected(
            TeleportRequest request
    ) implements TeleportRequestSelectionResult {

        public Selected {
            requireNonNull(request, "request");
        }

    }

    record Ambiguous(
            List<TeleportRequest> requests
    ) implements TeleportRequestSelectionResult {

        public Ambiguous {
            requests = List.copyOf(requireNonNull(requests, "requests"));
            if (requests.size() < 2) {
                throw new IllegalArgumentException("ambiguous selection needs at least two requests");
            }
        }

    }

}
