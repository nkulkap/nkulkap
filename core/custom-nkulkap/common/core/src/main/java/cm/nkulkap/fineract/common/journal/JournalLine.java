/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

import cm.nkulkap.fineract.common.money.Money;

/** Ligne d'écriture : un compte GL Fineract et un montant strictement positif. */
public record JournalLine(long glAccountId, Money amount) {}
