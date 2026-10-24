/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.veto;

import cm.nkulkap.fineract.common.error.NkulkapErrorCode;
import jakarta.annotation.PostConstruct;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.event.business.BusinessEventListener;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanDisbursalBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Étape 1.5 : veto avant décaissement. Refuse le décaissement d'un prêt dont l'externalId commence par
 * {@value #VETO_PREFIX}. L'exception levée dans l'écouteur « pre » annule toute la transaction de décaissement.
 *
 * <p>Exploration seulement : actif si {@code nkulkap.spike.disbursal-veto.enabled=true} (pile de développement).
 */
@Component
@ConditionalOnProperty(name = "nkulkap.spike.disbursal-veto.enabled", havingValue = "true")
public class DisbursalVetoSpikeListener implements BusinessEventListener<LoanDisbursalBusinessEvent> {

    public static final String VETO_PREFIX = "TEST-VETO";

    private final BusinessEventNotifierService businessEventNotifierService;

    public DisbursalVetoSpikeListener(BusinessEventNotifierService businessEventNotifierService) {
        this.businessEventNotifierService = businessEventNotifierService;
    }

    @PostConstruct
    void register() {
        businessEventNotifierService.addPreBusinessEventListener(LoanDisbursalBusinessEvent.class, this);
    }

    @Override
    public void onBusinessEvent(LoanDisbursalBusinessEvent event) {
        Loan loan = event.get();
        ExternalId externalId = loan.getExternalId();
        if (externalId != null && !externalId.isEmpty() && externalId.getValue().startsWith(VETO_PREFIX)) {
            // Le texte par défaut est destiné au développeur ; l'interface traduit la clé du code LOAN_DISBURSAL_VETOED
            // (fr, en).
            throw new GeneralPlatformDomainRuleException(
                    NkulkapErrorCode.LOAN_DISBURSAL_VETOED.key(),
                    "Disbursal vetoed by NKULKAP for loan " + externalId.getValue(),
                    externalId.getValue());
        }
    }
}
