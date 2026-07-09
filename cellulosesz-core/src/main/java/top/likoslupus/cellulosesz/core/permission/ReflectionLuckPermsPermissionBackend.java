package top.likoslupus.cellulosesz.core.permission;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.stream.Stream;

public final class ReflectionLuckPermsPermissionBackend implements PermissionBackend {

    @Override
    public boolean has(Object source, String permission) {
        var user = user(source);
        if (user.isEmpty()) return false;

        try {
            var cachedData = user.get()
                    .getClass()
                    .getMethod("getCachedData")
                    .invoke(user.get());
            var permissionData = cachedData.getClass()
                    .getMethod("getPermissionData")
                    .invoke(cachedData);
            var result = permissionData.getClass()
                    .getMethod("checkPermission", String.class)
                    .invoke(permissionData, permission);

            return tristate(result);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return false;
        }
    }

    @Override
    public Optional<String> stringOption(Object source, String key) {
        var user = user(source);
        if (user.isEmpty()) return Optional.empty();

        try {
            var cachedData = user.get()
                    .getClass()
                    .getMethod("getCachedData")
                    .invoke(user.get());
            var metaData = cachedData.getClass()
                    .getMethod("getMetaData")
                    .invoke(cachedData);

            Object value;
            try {
                value = metaData.getClass()
                        .getMethod("getMetaValue", String.class)
                        .invoke(metaData, key);
            } catch (NoSuchMethodException _) {
                value = metaData.getClass()
                        .getMethod("getMetaValue", String.class, String.class)
                        .invoke(metaData, key, null);
            }

            return Optional.ofNullable(value).map(String::valueOf);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    private Optional<Object> user(Object source) {
        var uuid = uuid(source);
        if (uuid.isEmpty()) return Optional.empty();

        try {
            var providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            var luckPerms = providerClass.getMethod("get").invoke(null);
            var userManager = luckPerms.getClass()
                    .getMethod("getUserManager")
                    .invoke(luckPerms);
            var loaded = userManager.getClass()
                    .getMethod("getUser", UUID.class)
                    .invoke(userManager, uuid.get());

            if (loaded != null) return Optional.of(loaded);

            var future = userManager.getClass()
                    .getMethod("loadUser", UUID.class)
                    .invoke(userManager, uuid.get());
            if (future instanceof CompletionStage<?> stage) {
                return Optional.ofNullable(stage.toCompletableFuture().join());
            }

            return Optional.empty();
        } catch (ClassNotFoundException |
                 IllegalAccessException |
                 InvocationTargetException |
                 NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    private boolean tristate(Object value) {
        try {
            var asBoolean = value.getClass()
                    .getMethod("asBoolean")
                    .invoke(value);
            if (asBoolean instanceof Boolean bool) return bool;
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException _) {
            // LuckPerms Tristate variants still expose an enum name.
        }

        return "TRUE".equalsIgnoreCase(String.valueOf(value));
    }

    private Optional<UUID> uuid(Object value) {
        var direct = invokeUuid(value);
        if (direct.isPresent()) return direct;

        var entity = invoke(value, "getEntity");
        if (entity.isPresent()) {
            direct = invokeUuid(entity.get());
            if (direct.isPresent()) return direct;
        }

        var player = invoke(value, "getPlayer");
        return player.flatMap(this::invokeUuid);
    }

    private Optional<UUID> invokeUuid(Object value) {
        return Stream.of("getUUID", "getUuid", "uuid", "id")
                .map(method -> invoke(value, method))
                .flatMap(Optional::stream)
                .filter(UUID.class::isInstance)
                .map(UUID.class::cast)
                .findFirst()
                .or(() -> {
                    var profile = invoke(value, "getGameProfile");
                    return profile.flatMap(p ->
                            Stream.of("id", "getId")
                                    .map(method -> invoke(p, method))
                                    .flatMap(Optional::stream)
                                    .filter(UUID.class::isInstance)
                                    .map(UUID.class::cast)
                                    .findFirst()
                    );
                });
    }

    private Optional<Object> invoke(Object value, String method) {
        try {
            return Optional.ofNullable(value.getClass().getMethod(method).invoke(value));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

}
