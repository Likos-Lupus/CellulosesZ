# Java consumer example

The API is a Kotlin library, but every entry point is Java-friendly: `fun interface` callbacks are
Java SAMs and the DTOs are plain classes. This is the shape proven by
`modules/api/src/test/java/.../JavaApiUsage.java`.

```java
import top.likoslupus.cellulosesz.api.*;

public final class MyIntegration {

    public void connect(CellulosesZApiProvider provider) {
        var subscription = provider.whenReady(api -> {
            // Called on (or forwarded to) the server thread once the session is ready.
            ApiQuery<Boolean> result = api.getVisibility().canSee(viewer, target);
            if (result instanceof ApiQuery.Available) {
                boolean visible = ((ApiQuery.Available<Boolean>) result).getValue();
            }
        });
    }

    public Subscription observe(CellulosesZApi api) {
        return api.getTeleports().subscribe(event -> {
            ApiPosition to = event.getTo(); // confirmed destination
        });
    }

}
```

Compile against `compileOnly("top.likoslupus:cellulosesz-api:0.1.0-SNAPSHOT")` and let the
CellulosesZ mod jar provide the classes at runtime.

## Granting an economy reward (Java)

```java
import top.likoslupus.cellulosesz.api.CellulosesZApi;
import top.likoslupus.cellulosesz.api.actor.AddonAccess;
import top.likoslupus.cellulosesz.api.economy.AddonEconomyGrantRequest;
import top.likoslupus.cellulosesz.api.economy.EconomyAmount;

import java.math.BigDecimal;
import java.util.UUID;

public void grantQuestReward(
        CellulosesZApi api,
        AddonAccess access,
        UUID playerId,
        String completionId
) {
    api.getEconomy().grantByAddon(
            new AddonEconomyGrantRequest(
                    access,
                    playerId,
                    new EconomyAmount(new BigDecimal("125.50")),
                    "quest:dragon:" + playerId + ":" + completionId,
                    "quest.reward",
                    null
            ),
            result -> {
                // result is an ApiCallResult<EconomyTransactionOutcome>; reuse the same requestKey on retry.
            }
    );
}
```
