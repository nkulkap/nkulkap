/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cm.nkulkap.fineract.common.audit.AuditChain.Stored;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditChainTest {

    private static AuditEntry entry(int i) {
        return new AuditEntry(Instant.parse("2026-10-21T08:00:00Z").plusSeconds(i), "fictif.caissier", "TOURNEE_RAPPROCHER",
                "tournee", String.valueOf(500 + i), UUID.fromString("0192f1e2-1111-7aaa-8bbb-00000000000" + i),
                "{\"ecart\":-500}");
    }

    private static List<Stored> chain(int n) {
        List<Stored> rows = new ArrayList<>();
        String previous = AuditChain.GENESIS;
        for (int i = 0; i < n; i++) {
            String hash = AuditChain.hash(previous, entry(i));
            rows.add(new Stored(entry(i), previous, hash));
            previous = hash;
        }
        return rows;
    }

    @Test
    void hashIsDeterministicAndDependsOnThePreviousLine() {
        String h1 = AuditChain.hash(AuditChain.GENESIS, entry(1));
        assertEquals(h1, AuditChain.hash(AuditChain.GENESIS, entry(1)));
        assertEquals(64, h1.length());
        assertNotEquals(h1, AuditChain.hash(h1, entry(1)));
    }

    @Test
    void distinguishesNullFromEmpty() {
        Instant t = Instant.parse("2026-10-21T08:00:00Z");
        AuditEntry withNull = new AuditEntry(t, "a", "b", "c", null, null, null);
        AuditEntry withEmpty = new AuditEntry(t, "a", "b", "c", "", null, "");
        assertNotEquals(AuditChain.hash(AuditChain.GENESIS, withNull), AuditChain.hash(AuditChain.GENESIS, withEmpty));
    }

    @Test
    void intactChainHasNoBreak() {
        assertEquals(OptionalInt.empty(), AuditChain.firstBreak(chain(5)));
        assertEquals(OptionalInt.empty(), AuditChain.firstBreak(List.of()));
    }

    @Test
    void detectsAModifiedLine() {
        List<Stored> rows = chain(5);
        Stored original = rows.get(2);
        AuditEntry altered = new AuditEntry(original.entry().occurredAt(), original.entry().actor(), original.entry().action(),
                original.entry().entityType(), original.entry().entityId(), original.entry().opId(), "{\"ecart\":0}");
        rows.set(2, new Stored(altered, original.previousHash(), original.hash()));
        assertEquals(OptionalInt.of(2), AuditChain.firstBreak(rows));
    }

    @Test
    void detectsADeletedLine() {
        List<Stored> rows = chain(5);
        rows.remove(1);
        assertEquals(OptionalInt.of(1), AuditChain.firstBreak(rows));
    }

    @Test
    void detectsALineRecomputedWithoutItsSuccessors() {
        // Un attaquant recalcule l'empreinte de la ligne modifiée : la ligne suivante ne se vérifie plus.
        List<Stored> rows = chain(4);
        Stored original = rows.get(1);
        AuditEntry altered = new AuditEntry(original.entry().occurredAt(), "fictif.admin", original.entry().action(),
                original.entry().entityType(), original.entry().entityId(), original.entry().opId(), original.entry().details());
        rows.set(1, new Stored(altered, original.previousHash(), AuditChain.hash(original.previousHash(), altered)));
        OptionalInt broken = AuditChain.firstBreak(rows);
        assertTrue(broken.isPresent());
        assertEquals(2, broken.getAsInt());
    }
}
