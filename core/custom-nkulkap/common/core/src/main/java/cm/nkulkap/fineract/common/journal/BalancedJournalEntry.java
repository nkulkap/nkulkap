/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

import cm.nkulkap.fineract.common.money.Money;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Écriture comptable équilibrée, prête pour {@code POST /journalentries} (FR-L3-04). Construite uniquement par
 * {@link JournalEntryBuilder}, qui garantit total des débits = total des crédits.
 */
public record BalancedJournalEntry(
        long officeId,
        LocalDate transactionDate,
        String referenceNumber,
        String comments,
        List<JournalLine> debits,
        List<JournalLine> credits,
        Money total) {

    public BalancedJournalEntry {
        debits = List.copyOf(debits);
        credits = List.copyOf(credits);
    }

    /** Corps JSON de {@code POST /journalentries}, montants en entiers. */
    public Map<String, Object> toFineractRequest() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("officeId", officeId);
        body.put("transactionDate", transactionDate.toString());
        body.put("currencyCode", total.currency().name());
        body.put("referenceNumber", referenceNumber);
        body.put("comments", comments);
        body.put("debits", lines(debits));
        body.put("credits", lines(credits));
        body.put("dateFormat", "yyyy-MM-dd");
        body.put("locale", "fr");
        return body;
    }

    private static List<Map<String, Object>> lines(List<JournalLine> lines) {
        return lines.stream()
                .map(l -> Map.<String, Object>of("glAccountId", l.glAccountId(), "amount", l.amount().amount()))
                .toList();
    }
}
