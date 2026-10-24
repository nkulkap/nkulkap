/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.journal;

/** Compte Numscript sans correspondance GL (rules/account_map.yaml) : l'écriture n'est pas produite. */
public class UnknownAccountException extends RuntimeException {

    public UnknownAccountException(String account) {
        super("Compte Numscript sans correspondance GL : " + account);
    }
}
