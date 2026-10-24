/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

/** Traduit un compte Numscript (ex. {@code emf:produits:commissions_collecte}) en identifiant de compte GL Fineract. */
@FunctionalInterface
public interface GlAccountResolver {

    /**
     * @throws UnknownAccountException si le compte n'a pas de correspondance
     */
    long glAccountIdFor(String numscriptAccount);
}
