/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.rules;

import cm.nkulkap.fineract.common.journal.Posting;
import java.util.List;
import java.util.Objects;

/** Résultat d'une évaluation nkulkap-rules : mouvements et empreinte SHA-256 de la règle appliquée. */
public record RulesResult(List<Posting> postings, String ruleSha256) {

    public RulesResult {
        postings = List.copyOf(Objects.requireNonNull(postings, "postings"));
        Objects.requireNonNull(ruleSha256, "ruleSha256");
    }
}
