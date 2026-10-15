package top.likoslupus.cellulosesz.api.sample;

import top.likoslupus.cellulosesz.api.*;

import java.util.UUID;

/**
 * Compile-time proof that the v0 API is consumable from plain Java without any Kotlin tooling. This
 * type is compiled by {@code compileTestJava}; it is not a runtime test.
 */
@SuppressWarnings("unused")
public final class JavaApiUsage {

    private JavaApiUsage() {
    }

    public static boolean visible(
            CellulosesZApi api,
            UUID viewer,
            UUID target
    ) {
        var result = api.getVisibility().canSee(viewer, target);
        return result instanceof ApiQuery.Available
                && ((ApiQuery.Available<Boolean>) result).getValue();
    }

    public static Subscription listen(CellulosesZApi api) {
        var listener = (TeleportListener) event -> {
            // event.getTo() is the confirmed destination; schedule any world access onto the
            // server thread.
        };
        return api.getTeleports().subscribe(listener);
    }

    public static Subscription onReady(CellulosesZApiProvider provider) {
        var listener = (ApiReadyListener) api -> {
            // The callback may run off the server thread; do not touch world state directly.
        };
        return provider.whenReady(listener);
    }

}
