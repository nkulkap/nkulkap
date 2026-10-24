/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.error;

import java.util.Arrays;
import java.util.Optional;

/** Catalogue des codes d'erreur NKULKAP : clé i18n stable (traduite en fr et en) et statut HTTP associé. */
public enum NkulkapErrorCode {
    MONEY_CURRENCY_MISMATCH("error.msg.nkulkap.money.currency.mismatch", 400),
    MONEY_OVERFLOW("error.msg.nkulkap.money.overflow", 400),
    MONEY_NOT_WHOLE_UNITS("error.msg.nkulkap.money.not.whole.units", 400),
    JOURNAL_EMPTY("error.msg.nkulkap.journal.empty", 400),
    JOURNAL_NON_POSITIVE_AMOUNT("error.msg.nkulkap.journal.non.positive.amount", 400),
    JOURNAL_SAME_ACCOUNT("error.msg.nkulkap.journal.same.account", 400),
    JOURNAL_UNKNOWN_ACCOUNT("error.msg.nkulkap.journal.unknown.account", 400),
    JOURNAL_UNBALANCED("error.msg.nkulkap.journal.unbalanced", 500),
    LOAN_DISBURSAL_VETOED("error.msg.nkulkap.loan.disbursal.vetoed", 403),
    RULES_UNAVAILABLE("error.msg.nkulkap.rules.unavailable", 503),
    RULES_INVALID_RESPONSE("error.msg.nkulkap.rules.invalid.response", 502),
    RULES_REJECTED("error.msg.nkulkap.rules.rejected", 422),
    AUDIT_CHAIN_BROKEN("error.msg.nkulkap.audit.chain.broken", 500);

    private final String key;
    private final int httpStatus;

    NkulkapErrorCode(String key, int httpStatus) {
        this.key = key;
        this.httpStatus = httpStatus;
    }

    /** Clé i18n stable, jamais modifiée une fois publiée. */
    public String key() {
        return key;
    }

    public int httpStatus() {
        return httpStatus;
    }

    /** Retrouve un code à partir de sa clé i18n. */
    public static Optional<NkulkapErrorCode> fromKey(String key) {
        return Arrays.stream(values()).filter(c -> c.key.equals(key)).findFirst();
    }
}
