/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

import cm.nkulkap.fineract.common.money.Money;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Transforme les mouvements d'une règle Numscript en <b>une</b> écriture comptable équilibrée (FR-L3-04) : chaque
 * mouvement débite le compte GL de sa source et crédite celui de sa destination. Les lignes d'un même compte et d'un
 * même sens sont additionnées, dans l'ordre de première apparition ; débit et crédit d'un même compte ne sont pas
 * compensés. {@code referenceNumber} = {@code op_id}.
 */
public final class JournalEntryBuilder {

    private final GlAccountResolver resolver;

    public JournalEntryBuilder(GlAccountResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver");
    }

    public BalancedJournalEntry build(
            long officeId, LocalDate transactionDate, UUID opId, String comments, List<Posting> postings) {
        Objects.requireNonNull(transactionDate, "transactionDate");
        Objects.requireNonNull(opId, "opId");
        if (postings == null || postings.isEmpty()) {
            throw new IllegalArgumentException("Aucun mouvement : écriture vide refusée");
        }
        Map<Long, Money> debits = new LinkedHashMap<>();
        Map<Long, Money> credits = new LinkedHashMap<>();
        Money total = null;
        for (Posting p : postings) {
            if (!p.amount().isPositive()) {
                throw new IllegalArgumentException("Montant non positif : " + p);
            }
            if (p.source().equals(p.destination())) {
                throw new IllegalArgumentException("Source et destination identiques : " + p);
            }
            // plus() refuse un mélange de devises.
            total = total == null ? p.amount() : total.plus(p.amount());
            debits.merge(resolver.glAccountIdFor(p.source()), p.amount(), Money::plus);
            credits.merge(resolver.glAccountIdFor(p.destination()), p.amount(), Money::plus);
        }
        List<JournalLine> debitLines = toLines(debits);
        List<JournalLine> creditLines = toLines(credits);
        // Équilibre garanti par construction ; vérifié quand même avant de produire l'écriture.
        if (!sum(debitLines).equals(total) || !sum(creditLines).equals(total)) {
            throw new IllegalStateException("Écriture déséquilibrée : " + debitLines + " / " + creditLines);
        }
        return new BalancedJournalEntry(
                officeId, transactionDate, opId.toString(), comments, debitLines, creditLines, total);
    }

    private static List<JournalLine> toLines(Map<Long, Money> byAccount) {
        List<JournalLine> lines = new ArrayList<>(byAccount.size());
        byAccount.forEach((account, amount) -> lines.add(new JournalLine(account, amount)));
        return lines;
    }

    private static Money sum(List<JournalLine> lines) {
        return lines.stream().map(JournalLine::amount).reduce(Money::plus).orElseThrow();
    }
}
