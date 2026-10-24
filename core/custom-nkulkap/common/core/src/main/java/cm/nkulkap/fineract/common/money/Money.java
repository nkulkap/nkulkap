/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.money;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Montant en unités entières d'une devise sans subdivision (XAF). Jamais de flottant : toute opération est exacte
 * et lève {@link ArithmeticException} en cas de dépassement au lieu de boucler.
 */
public record Money(long amount, Currency currency) implements Comparable<Money> {

    public Money {
        Objects.requireNonNull(currency, "currency");
    }

    public static Money of(long amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money xaf(long amount) {
        return new Money(amount, Currency.XAF);
    }

    /** Convertit un décimal renvoyé par Fineract (ex. 150000.000000) ; refuse toute partie fractionnaire. */
    public static Money fromDecimal(BigDecimal value, Currency currency) {
        return new Money(value.stripTrailingZeros().longValueExact(), currency);
    }

    public Money plus(Money other) {
        return new Money(Math.addExact(amount, sameCurrency(other).amount), currency);
    }

    public Money minus(Money other) {
        return new Money(Math.subtractExact(amount, sameCurrency(other).amount), currency);
    }

    public Money negate() {
        return new Money(Math.negateExact(amount), currency);
    }

    public boolean isPositive() {
        return amount > 0;
    }

    public boolean isNegative() {
        return amount < 0;
    }

    public boolean isZero() {
        return amount == 0;
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(amount);
    }

    @Override
    public int compareTo(Money other) {
        return Long.compare(amount, sameCurrency(other).amount);
    }

    @Override
    public String toString() {
        return amount + " " + currency;
    }

    private Money sameCurrency(Money other) {
        if (other.currency != currency) {
            throw new IllegalArgumentException("Devises différentes : " + currency + " et " + other.currency);
        }
        return other;
    }
}
