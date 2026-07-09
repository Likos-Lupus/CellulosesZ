package top.likoslupus.cellulosesz.modules.user.service;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

final class PlayerIdentity {

    private PlayerIdentity() {
    }

    static Optional<UUID> uuid(Object player) {
        return Stream.of("getUUID", "getUuid", "uuid", "id")
                .map(method -> invoke(player, method))
                .flatMap(Optional::stream)
                .filter(UUID.class::isInstance)
                .map(UUID.class::cast)
                .findFirst()
                .or(() -> {
                    Optional<Object> profile = invoke(player, "getGameProfile");
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

    private static Optional<Object> invoke(Object value, String method) {
        if (value == null) return Optional.empty();
        try {
            return Optional.ofNullable(value.getClass().getMethod(method).invoke(value));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException exception) {
            return Optional.empty();
        }
    }

    static Optional<String> name(Object player) {
        return invoke(player, "getGameProfile")
                .flatMap(profile ->
                        Stream.of("name", "getName")
                                .map(m -> invoke(profile, m))
                                .flatMap(Optional::stream)
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .filter(s -> !s.isBlank())
                                .findFirst()
                )
                .or(() -> invoke(player, "getName")
                        .flatMap(component -> {
                            var fromGetString = invoke(component, "getString")
                                    .filter(String.class::isInstance)
                                    .map(String.class::cast)
                                    .filter(s -> !s.isBlank());
                            if (fromGetString.isPresent()) return fromGetString;

                            var plain = String.valueOf(component);
                            return plain.isBlank()
                                    ? Optional.<String>empty()
                                    : Optional.of(plain);
                        })
                );
    }

}
