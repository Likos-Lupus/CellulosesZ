# Java consumer example

The API is a Kotlin library, but every entry point is Java-friendly: `fun interface` callbacks are
Java SAMs and the DTOs are plain classes. This is the shape proven by
`modules/api/src/test/java/.../JavaApiUsage.java`.

```java
import top.likoslupus.cellulosesz.api.*;

public final class MyIntegration {

    public void connect(CellulosesZApiProvider provider) {
        Subscription subscription = provider.whenReady(api -> {
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
