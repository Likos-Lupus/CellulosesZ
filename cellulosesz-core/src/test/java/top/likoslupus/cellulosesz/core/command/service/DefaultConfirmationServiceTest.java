package top.likoslupus.cellulosesz.core.command.service;

import org.junit.jupiter.api.Test;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationConsumeStatus;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationKey;
import top.likoslupus.cellulosesz.api.command.service.ConfirmationToken;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DefaultConfirmationServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000501");
    private static final ConfirmationKey<String> TEXT = new ConfirmationKey<>("test", String.class);

    @Test
    void consumesOnlyMatchingTokenAndType() {
        var clock = new AtomicLong(1_000L);
        var service = new DefaultConfirmationService(clock::get, () -> "token1");
        var token = service.request(PLAYER, TEXT, "payload", Duration.ofSeconds(10));

        assertEquals(
                ConfirmationConsumeStatus.TOKEN_MISMATCH,
                service.consume(PLAYER, TEXT, new ConfirmationToken("wrong1")).status()
        );

        var wrongType = service.consume(
                PLAYER,
                new ConfirmationKey<>("test", Integer.class),
                token
        );
        assertEquals(ConfirmationConsumeStatus.PAYLOAD_TYPE_MISMATCH, wrongType.status());

        var consumed = service.consume(PLAYER, TEXT, token);
        assertTrue(consumed.consumed());
        assertEquals("payload", consumed.payload().orElseThrow());
        assertEquals(
                ConfirmationConsumeStatus.NOT_FOUND,
                service.consume(PLAYER, TEXT, token).status()
        );
    }

    @Test
    void distinguishesExpiredAndMissingEntries() {
        var clock = new AtomicLong(5_000L);
        var service = new DefaultConfirmationService(clock::get, () -> "token2");
        var token = service.request(PLAYER, TEXT, "payload", Duration.ofMillis(25));

        clock.addAndGet(25L);
        assertEquals(
                ConfirmationConsumeStatus.EXPIRED,
                service.consume(PLAYER, TEXT, token).status()
        );
        assertEquals(
                ConfirmationConsumeStatus.NOT_FOUND,
                service.consume(PLAYER, TEXT, token).status()
        );
    }

}
