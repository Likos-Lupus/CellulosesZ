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
        ApiQuery.NotReady, ApiQuery.Stopping -> false
    }

/** Observe confirmed teleports; `event.to` is the position the server actually committed. */
fun observeTeleports(api: CellulosesZApi): Subscription =
    api.teleports.subscribe { event ->
        // Runs on the server thread. Do not start another teleport from here.
        println("cellulosesz_teleport subject=${event.subjectId} kind=${event.kind} to=${event.to}")
    }
```

Close the returned `Subscription` on your own unload/stop path:

```kotlin
subscription.close()
```
