/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditEntryTest {

    private static final Instant T = Instant.parse("2026-10-21T08:00:00Z");

    /** Consomme le résultat d'une construction dont on attend qu'elle échoue (SpotBugs refuse une valeur ignorée). */
    private static void use(Object result) {
        assertFalse(result == null);
    }

    private static String messageOf(String actor, String action, String entityType) {
        return assertThrows(
                        IllegalArgumentException.class,
                        () -> use(new AuditEntry(T, actor, action, entityType, null, null, null)))
                .getMessage();
    }

    @Test
    void refusesMissingOrBlankMandatoryFields() {
        for (String bad : new String[] {null, "", "   "}) {
            assertEquals("actor obligatoire", messageOf(bad, "a", "t"));
            assertEquals("action obligatoire", messageOf("x", bad, "t"));
            assertEquals("entityType obligatoire", messageOf("x", "a", bad));
        }
    }

    @Test
    void refusesMissingTimestamp() {
        assertThrows(NullPointerException.class, () -> use(new AuditEntry(null, "x", "a", "t", null, null, null)));
    }
}
