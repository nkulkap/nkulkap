/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.money;

/** Devises gérées par NKULKAP, toutes sans subdivision : un montant est un nombre entier d'unités. */
public enum Currency {
    /** Franc CFA BEAC. */
    XAF,
    /** Devise de test, pour vérifier le refus des mélanges de devises ; jamais utilisée en production. */
    TEST_ONLY
}
