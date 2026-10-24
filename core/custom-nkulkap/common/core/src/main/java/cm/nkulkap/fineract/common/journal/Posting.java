/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

import cm.nkulkap.fineract.common.money.Money;
import java.util.Objects;

/** Mouvement produit par une règle Numscript : la source est débitée, la destination créditée. */
public record Posting(String source, String destination, Money amount) {

    public Posting {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(amount, "amount");
    }
}
