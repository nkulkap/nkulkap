/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.id;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Générateur d'UUID version 7 (RFC 9562) : 48 bits d'horodatage Unix en millisecondes, puis 74 bits aléatoires.
 * Les identifiants d'un même générateur sont strictement croissants, y compris dans une même milliseconde et si
 * l'horloge recule : le compteur de 12 bits (champ rand_a) prend le relais (méthode 1 de la RFC, section 6.2).
 *
 * <p>Sert à créer les {@code op_id} (ADR-016) et les identifiants d'événements. Sûr entre fils d'exécution.
 */
public final class UuidV7 {

    private static final int COUNTER_BITS = 12;
    private static final long COUNTER_MAX = (1L << COUNTER_BITS) - 1;

    private final Clock clock;
    private final SecureRandom random;
    private long lastMillis = -1;
    private long counter;

    public UuidV7() {
        this(Clock.systemUTC(), new SecureRandom());
    }

    UuidV7(Clock clock, SecureRandom random) {
        this.clock = clock;
        this.random = random;
    }

    public synchronized UUID next() {
        long now = clock.millis();
        if (now > lastMillis) {
            lastMillis = now;
            // Départ aléatoire dans la moitié basse : laisse au moins 2048 incréments dans la milliseconde.
            counter = random.nextInt(1 << (COUNTER_BITS - 1));
        } else if (counter < COUNTER_MAX) {
            counter++;
        } else {
            // Compteur épuisé (ou horloge en retard) : on avance l'horodatage logique d'une milliseconde.
            lastMillis++;
            counter = random.nextInt(1 << (COUNTER_BITS - 1));
        }
        long msb = (lastMillis << 16) | (0x7L << 12) | counter;
        long lsb = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(msb, lsb);
    }

    /** Horodatage porté par un UUIDv7. */
    public static Instant timestampOf(UUID uuid) {
        if (uuid.version() != 7) {
            throw new IllegalArgumentException("UUID de version " + uuid.version() + ", version 7 attendue : " + uuid);
        }
        return Instant.ofEpochMilli(uuid.getMostSignificantBits() >>> 16);
    }
}
