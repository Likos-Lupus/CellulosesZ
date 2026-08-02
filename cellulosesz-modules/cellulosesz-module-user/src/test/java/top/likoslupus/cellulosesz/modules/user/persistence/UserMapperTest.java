package top.likoslupus.cellulosesz.modules.user.persistence;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.user.CellUser;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class UserMapperTest {

    @Test
    void roundTripsImmutableUser() {
        var user = CellUser.create(UUID.fromString("00000000-0000-0000-0000-000000000123"))
                .withLastKnownName("Player")
                .withCooldowns(Map.of("daily", 42L));

        assertEquals(user, UserMapper.toDomain(UserMapper.fromDomain(user)));
    }

    @Test
    void rejectsInvalidPersistedUuid() {
        var document = UserMapper.fromDomain(CellUser.create(UUID.randomUUID()));
        document.uuid = "invalid";

        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> UserMapper.toDomain(document)
        );
        assertEquals("Invalid persisted user document", failure.getMessage());
    }

}
