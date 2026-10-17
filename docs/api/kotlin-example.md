# Kotlin consumer example

```kotlin
import top.likoslupus.cellulosesz.api.ApiQuery
import top.likoslupus.cellulosesz.api.CellulosesZApi
import top.likoslupus.cellulosesz.api.Subscription
import java.util.UUID

/** Read visibility for a viewer/target pair without ever seeing a Minecraft object. */
fun displayIfVisible(api: CellulosesZApi, viewer: UUID, target: UUID): Boolean =
    when (val result = api.visibility.canSee(viewer, target)) {
        is ApiQuery.Available -> result.value
        ApiQuery.NotReady,
        ApiQuery.Stopping -> false
    }

/** Observe confirmed teleports; `event.to` is the position the server actually committed. */
fun observeTeleports(api: CellulosesZApi): Subscription =
    api.teleports.subscribe {
        // Runs on the server thread. Do not start another teleport from here.
        println("cellulosesz_teleport subject=${it.subjectId} kind=${it.kind} to=${it.to}")
    }
```

Close the returned `Subscription` on your own unload/stop path:

```kotlin
subscription.close()
```

## Granting an economy reward (idempotent)

`access` must be the handle returned by `registerAddon`; a self-made `AddonAccess` is rejected
server-side. Reuse the **same** `requestKey` on every retry so the reward is granted at most once.

```kotlin
import top.likoslupus.cellulosesz.api.actor.AddonAccess
import top.likoslupus.cellulosesz.api.common.ApiCallResult
import top.likoslupus.cellulosesz.api.economy.AddonEconomyGrantRequest
import top.likoslupus.cellulosesz.api.economy.EconomyAmount
import java.math.BigDecimal
import java.util.UUID

fun grantQuestReward(
    api: CellulosesZApi,
    access: AddonAccess,
    playerId: UUID,
    completionId: String
) {
    api.economy.grantByAddon(
        AddonEconomyGrantRequest(
            access = access,
            playerId = playerId,
            amount = EconomyAmount(BigDecimal("125.50")),
            requestKey = "quest:dragon:$playerId:$completionId",
            reasonCode = "quest.reward",
        )
    ) {
        when (it) {
            is ApiCallResult.Completed -> {
                // A committed or already-committed outcome means the player has the reward.
            }

            is ApiCallResult.Failed -> {
                // Transient failure: retry later with the SAME requestKey (result.diagnosticId logs it).
            }

            ApiCallResult.NotReady,
            ApiCallResult.Stopping,
            is ApiCallResult.Unavailable -> {
                // Not available for this session; retry after the next ready signal.
            }
        }
    }
}
```
