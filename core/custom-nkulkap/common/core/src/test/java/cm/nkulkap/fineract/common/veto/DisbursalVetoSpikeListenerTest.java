/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.veto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanDisbursalBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.junit.jupiter.api.Test;

class DisbursalVetoSpikeListenerTest {

    private final BusinessEventNotifierService notifier = mock(BusinessEventNotifierService.class);
    private final DisbursalVetoSpikeListener listener = new DisbursalVetoSpikeListener(notifier);

    private static LoanDisbursalBusinessEvent eventFor(ExternalId externalId) {
        Loan loan = mock(Loan.class);
        when(loan.getExternalId()).thenReturn(externalId);
        return new LoanDisbursalBusinessEvent(loan);
    }

    @Test
    void registersAsPreDisbursalListener() {
        listener.register();
        verify(notifier).addPreBusinessEventListener(LoanDisbursalBusinessEvent.class, listener);
    }

    @Test
    void vetoesLoanMarkedTestVeto() {
        GeneralPlatformDomainRuleException e = assertThrows(
                GeneralPlatformDomainRuleException.class,
                () -> listener.onBusinessEvent(eventFor(new ExternalId("TEST-VETO-1"))));
        assertEquals(DisbursalVetoSpikeListener.ERROR_CODE, e.getGlobalisationMessageCode());
    }

    @Test
    void letsOtherLoansThrough() {
        assertDoesNotThrow(() -> listener.onBusinessEvent(eventFor(new ExternalId("SEED-PRT-01"))));
        assertDoesNotThrow(() -> listener.onBusinessEvent(eventFor(ExternalId.empty())));
    }
}
