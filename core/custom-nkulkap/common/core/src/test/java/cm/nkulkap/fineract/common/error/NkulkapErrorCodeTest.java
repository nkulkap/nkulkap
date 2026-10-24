/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class NkulkapErrorCodeTest {

    @Test
    void keysAreUniqueAndPrefixed() {
        Set<String> keys = Arrays.stream(NkulkapErrorCode.values())
                .map(NkulkapErrorCode::key)
                .collect(Collectors.toSet());
        assertEquals(NkulkapErrorCode.values().length, keys.size());
        for (NkulkapErrorCode c : NkulkapErrorCode.values()) {
            assertTrue(c.key().startsWith("error.msg.nkulkap."), c.key());
            assertTrue(c.httpStatus() >= 400 && c.httpStatus() < 600, c.name());
        }
    }

    @Test
    void fromKeyFindsCodes() {
        assertEquals(
                Optional.of(NkulkapErrorCode.RULES_REJECTED),
                NkulkapErrorCode.fromKey("error.msg.nkulkap.rules.rejected"));
        assertEquals(Optional.empty(), NkulkapErrorCode.fromKey("error.msg.inconnu"));
    }

    @Test
    void exceptionCarriesCodeAndArgs() {
        NkulkapException e = new NkulkapException(NkulkapErrorCode.JOURNAL_EMPTY, "empty", "a", 2);
        assertEquals(NkulkapErrorCode.JOURNAL_EMPTY, e.code());
        assertEquals(2, e.args().length);
        assertEquals("empty", e.getMessage());
        NkulkapException c = new NkulkapException(NkulkapErrorCode.JOURNAL_EMPTY, "x", new RuntimeException("c"));
        assertEquals(0, c.args().length);
        assertEquals("c", c.getCause().getMessage());
    }
}
