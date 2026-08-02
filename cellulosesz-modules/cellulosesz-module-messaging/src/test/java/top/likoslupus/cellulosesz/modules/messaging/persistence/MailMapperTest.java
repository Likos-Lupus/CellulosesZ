package top.likoslupus.cellulosesz.modules.messaging.persistence;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.messaging.MailMessage;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class MailMapperTest {

    @Test
    void roundTripsImmutableMailMessage() {
        var message = new MailMessage(
                UUID.fromString("00000000-0000-0000-0000-000000000111"),
                UUID.fromString("00000000-0000-0000-0000-000000000222"),
                "Sender",
                UUID.fromString("00000000-0000-0000-0000-000000000333"),
                "Hello",
                1234L,
                5678L,
                true
        );

        assertEquals(message, MailMapper.toDomain(MailMapper.fromDomain(message)));
    }

    @Test
    void rejectsInvalidPersistedRecipientWithMessageContext() {
        var document = MailMapper.fromDomain(new MailMessage(
                UUID.fromString("00000000-0000-0000-0000-000000000111"),
                null,
                "Console",
                UUID.fromString("00000000-0000-0000-0000-000000000333"),
                "Hello",
                1234L,
                null,
                false
        ));
        document.toUuid = "invalid";

        var failure = assertThrows(
                IllegalArgumentException.class,
                () -> MailMapper.toDomain(document)
        );
        assertEquals(
                "Invalid persisted mail message 00000000-0000-0000-0000-000000000111",
                failure.getMessage()
        );
    }

}
