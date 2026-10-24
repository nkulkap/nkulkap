/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.id;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    /** Horloge pilotée par le test. */
    private static final class TestClock extends Clock {
        final AtomicLong millis = new AtomicLong(1_761_065_530_123L);

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }
    }

    @Test
    void hasVersion7VariantAndTimestamp() {
        TestClock clock = new TestClock();
        UUID id = new UuidV7(clock, new SecureRandom()).next();
        assertEquals(7, id.version());
        assertEquals(2, id.variant());
        assertEquals(Instant.ofEpochMilli(1_761_065_530_123L), UuidV7.timestampOf(id));
    }

    @Test
    void isStrictlyIncreasingWithinTheSameMillisecond() {
        UuidV7 gen = new UuidV7(new TestClock(), new SecureRandom());
        UUID previous = gen.next();
        // Plus d'identifiants que le compteur n'en loge : l'horodatage logique avance sans régresser.
        for (int i = 0; i < 10_000; i++) {
            UUID next = gen.next();
            assertTrue(compareUnsigned(previous, next) < 0, previous + " >= " + next);
            previous = next;
        }
    }

    @Test
    void neverGoesBackwardsWhenTheClockDoes() {
        TestClock clock = new TestClock();
        UuidV7 gen = new UuidV7(clock, new SecureRandom());
        UUID before = gen.next();
        clock.millis.addAndGet(-5_000);
        UUID after = gen.next();
        assertTrue(compareUnsigned(before, after) < 0);
    }

    @Test
    void refusesOtherVersions() {
        assertThrows(IllegalArgumentException.class, () -> UuidV7.timestampOf(UUID.randomUUID()));
    }

    /** Ordre lexicographique des 128 bits, celui des chaînes et des colonnes uuid de PostgreSQL. */
    private static int compareUnsigned(UUID a, UUID b) {
        int c = Long.compareUnsigned(a.getMostSignificantBits(), b.getMostSignificantBits());
        return c != 0 ? c : Long.compareUnsigned(a.getLeastSignificantBits(), b.getLeastSignificantBits());
    }
}
