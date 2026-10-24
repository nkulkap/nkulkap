/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.money;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void holdsWholeUnitsOfXaf() {
        Money m = Money.xaf(150_000);
        assertEquals(150_000L, m.amount());
        assertEquals(Currency.XAF, m.currency());
        assertEquals("150000 XAF", m.toString());
    }

    @Test
    void addsAndSubtractsExactly() {
        assertEquals(Money.xaf(3_100), Money.xaf(1_240).plus(Money.xaf(1_860)));
        assertEquals(Money.xaf(-500), Money.xaf(73_500).minus(Money.xaf(74_000)));
        assertEquals(Money.xaf(0), Money.xaf(1_000).minus(Money.xaf(1_000)));
    }

    @Test
    void refusesOverflowInsteadOfWrapping() {
        Money max = Money.xaf(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> max.plus(Money.xaf(1)));
        assertThrows(ArithmeticException.class, () -> Money.xaf(Long.MIN_VALUE).minus(Money.xaf(1)));
        assertThrows(ArithmeticException.class, () -> Money.xaf(Long.MIN_VALUE).negate());
    }

    @Test
    void refusesMixingCurrencies() {
        // Une seule devise aujourd'hui ; le contrôle protège le jour où une seconde apparaît.
        assertThrows(NullPointerException.class, () -> Money.of(1, null));
        assertThrows(IllegalArgumentException.class, () -> Money.xaf(1).plus(Money.of(1, Currency.TEST_ONLY)));
    }

    @Test
    void comparesAndReportsSign() {
        assertTrue(Money.xaf(1).isPositive());
        assertFalse(Money.xaf(0).isPositive());
        assertTrue(Money.xaf(0).isZero());
        assertTrue(Money.xaf(-1).isNegative());
        assertTrue(Money.xaf(2).compareTo(Money.xaf(10)) < 0);
        assertEquals(Money.xaf(5), Money.xaf(-5).negate());
    }

    @Test
    void convertsFromFineractDecimalsOnlyWhenExact() {
        // Fineract renvoie 150000.000000 : accepté ; 161.4 n'est pas un montant XAF valide.
        assertEquals(Money.xaf(150_000), Money.fromDecimal(new BigDecimal("150000.000000"), Currency.XAF));
        assertThrows(ArithmeticException.class, () -> Money.fromDecimal(new BigDecimal("161.4"), Currency.XAF));
        assertEquals(new BigDecimal("150000"), Money.xaf(150_000).toDecimal());
    }
}
