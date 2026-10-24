/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import cm.nkulkap.fineract.common.money.Currency;
import cm.nkulkap.fineract.common.money.Money;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class JournalEntryBuilderTest {

    // Correspondance FICTIVE de l'annexe B.3 (codes GL -> identifiants Fineract).
    private static final Map<String, Long> GL = Map.of(
            "emf:frais_a_repartir", 10L,
            "etat:tva_collectee", 20L,
            "agent:7", 30L,
            "emf:produits:commissions_momo", 40L);
    private static final UUID OP = UUID.fromString("0192f1e2-1111-7aaa-8bbb-000000000001");

    private static JournalEntryBuilder builder() {
        return new JournalEntryBuilder(account -> {
            Long id = GL.get(account);
            if (id == null) {
                throw new UnknownAccountException(account);
            }
            return id;
        });
    }

    @Test
    void turnsAnnexB2PostingsIntoOneBalancedEntry() {
        List<Posting> postings = List.of(
                new Posting("emf:frais_a_repartir", "etat:tva_collectee", Money.xaf(162)),
                new Posting("emf:frais_a_repartir", "agent:7", Money.xaf(252)),
                new Posting("emf:frais_a_repartir", "emf:produits:commissions_momo", Money.xaf(586)));

        BalancedJournalEntry entry =
                builder().build(2L, LocalDate.of(2026, 10, 21), OP, "Frais de retrait momo", postings);

        // Débit du compte de liaison : une seule ligne, somme des sources identiques.
        assertEquals(List.of(new JournalLine(10L, Money.xaf(1_000))), entry.debits());
        assertEquals(
                List.of(
                        new JournalLine(20L, Money.xaf(162)),
                        new JournalLine(30L, Money.xaf(252)),
                        new JournalLine(40L, Money.xaf(586))),
                entry.credits());
        assertEquals(Money.xaf(1_000), entry.total());
        assertEquals(OP.toString(), entry.referenceNumber());
    }

    @Test
    void producesTheFineractRequestBody() {
        BalancedJournalEntry entry = builder()
                .build(
                        2L,
                        LocalDate.of(2026, 10, 21),
                        OP,
                        "B.2",
                        List.of(new Posting("emf:frais_a_repartir", "agent:7", Money.xaf(252))));

        Map<String, Object> body = entry.toFineractRequest();

        assertEquals(2L, body.get("officeId"));
        assertEquals("2026-10-21", body.get("transactionDate"));
        assertEquals("XAF", body.get("currencyCode"));
        assertEquals(OP.toString(), body.get("referenceNumber"));
        assertEquals("yyyy-MM-dd", body.get("dateFormat"));
        assertEquals(List.of(Map.of("glAccountId", 10L, "amount", 252L)), body.get("debits"));
        assertEquals(List.of(Map.of("glAccountId", 30L, "amount", 252L)), body.get("credits"));
    }

    @Test
    void refusesInvalidPostings() {
        LocalDate day = LocalDate.of(2026, 10, 21);
        JournalEntryBuilder b = builder();
        assertThrows(IllegalArgumentException.class, () -> b.build(2L, day, OP, "vide", List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> b.build(
                        2L, day, OP, "zéro", List.of(new Posting("emf:frais_a_repartir", "agent:7", Money.xaf(0)))));
        assertThrows(
                IllegalArgumentException.class,
                () -> b.build(
                        2L,
                        day,
                        OP,
                        "négatif",
                        List.of(new Posting("emf:frais_a_repartir", "agent:7", Money.xaf(-1)))));
        assertThrows(
                IllegalArgumentException.class,
                () -> b.build(2L, day, OP, "même compte", List.of(new Posting("agent:7", "agent:7", Money.xaf(10)))));
        assertThrows(
                IllegalArgumentException.class,
                () -> b.build(
                        2L,
                        day,
                        OP,
                        "deux devises",
                        List.of(
                                new Posting("emf:frais_a_repartir", "agent:7", Money.xaf(10)),
                                new Posting("emf:frais_a_repartir", "agent:7", Money.of(10, Currency.TEST_ONLY)))));
        assertThrows(
                UnknownAccountException.class,
                () -> b.build(
                        2L, day, OP, "compte inconnu", List.of(new Posting("emf:inconnu", "agent:7", Money.xaf(10)))));
    }
}
