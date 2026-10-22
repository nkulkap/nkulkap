/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 */
package cm.nkulkap.spikes;

import static cm.nkulkap.spikes.FineractApi.dated;
import static org.junit.jupiter.api.Assertions.assertEquals;

import cm.nkulkap.spikes.FineractApi.Response;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Étape 1.5 : un écouteur « pre » sur LoanDisbursalBusinessEvent (module common, DisbursalVetoSpikeListener) refuse le
 * décaissement d'un prêt dont l'externalId commence par TEST-VETO. Exige la pile avec
 * NKULKAP_SPIKE_DISBURSAL_VETO_ENABLED=true et le seed.
 */
class DisbursalVetoSpikeIT {

    private static final FineractApi API = new FineractApi();
    private static long cash;

    @BeforeAll
    static void setUp() throws Exception {
        API.awaitReady(Duration.ofMinutes(1));
        cash = API.cashPaymentTypeId();
    }

    private static long approvedLoan(String externalId) throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        long client = API.newClient(tag);
        long product = API.productId("/loanproducts", "FICTIF Crédit ordinaire");
        Map<String, Object> body = dated(
                "clientId", client, "productId", product, "externalId", externalId, "loanType", "individual",
                "principal", 300_000, "loanTermFrequency", 6, "loanTermFrequencyType", 2, "numberOfRepayments", 6,
                "repaymentEvery", 1, "repaymentFrequencyType", 2, "interestRatePerPeriod", 2, "amortizationType", 1,
                "interestType", 1, "interestCalculationPeriodType", 1,
                "transactionProcessingStrategyCode", "mifos-standard-strategy",
                "expectedDisbursementDate", "2026-01-02", "submittedOnDate", "2026-01-02");
        long id = API.ok(API.post("/loans", body, null), "loan").path("loanId").asLong();
        API.ok(API.post("/loans/" + id + "?command=approve", dated("approvedOnDate", "2026-01-02"), null), "approve");
        return id;
    }

    private static Response disburse(long loan) throws Exception {
        return API.post("/loans/" + loan + "?command=disburse",
                dated("actualDisbursementDate", "2026-01-02", "transactionAmount", 300_000, "paymentTypeId", cash),
                FineractApi.newKey());
    }

    @Test
    void vetoedLoanIsNotDisbursedAndNothingIsWritten() throws Exception {
        long loan = approvedLoan("TEST-VETO-" + UUID.randomUUID());

        Response r = disburse(loan);
        System.out.println("(veto) décaissement refusé : " + r);

        assertEquals(403, r.status());
        JsonNode error = r.body().path("errors").path(0);
        assertEquals("error.msg.nkulkap.loan.disbursal.vetoed", error.path("userMessageGlobalisationCode").asText());
        JsonNode l = API.ok(API.get("/loans/" + loan + "?associations=transactions"), "loan");
        assertEquals("Approved", l.path("status").path("value").asText(), "le prêt reste approuvé");
        assertEquals(0, l.path("transactions").size(), "aucune transaction");
        assertEquals(0, API.ok(API.get("/journalentries?loanId=" + loan), "journal").path("totalFilteredRecords").asInt(),
                "aucune écriture");
        assertEquals(true, l.path("timeline").path("actualDisbursementDate").isMissingNode(), "date de décaissement non enregistrée");
    }

    @Test
    void otherLoansAreDisbursed() throws Exception {
        long loan = approvedLoan("SPIKE-PRT-" + UUID.randomUUID());

        Response r = disburse(loan);
        System.out.println("(témoin) décaissement : " + r);

        assertEquals(200, r.status());
        assertEquals("Active", API.ok(API.get("/loans/" + loan), "loan").path("status").path("value").asText());
        assertEquals(2, API.ok(API.get("/journalentries?loanId=" + loan), "journal").path("totalFilteredRecords").asInt());
    }
}
