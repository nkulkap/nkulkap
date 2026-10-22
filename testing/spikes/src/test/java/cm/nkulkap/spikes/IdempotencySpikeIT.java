/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 */
package cm.nkulkap.spikes;

import static cm.nkulkap.spikes.FineractApi.dated;
import static cm.nkulkap.spikes.FineractApi.newKey;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cm.nkulkap.spikes.FineractApi.Response;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Étape 1.2 : comportement de l'en-tête Idempotency-Key de Fineract. Fige ce qui est décrit dans
 * docs/specs/idempotence-fineract.md. Exige la pile locale et le seed (make up && make seed).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IdempotencySpikeIT {

    private static final FineractApi API = new FineractApi();
    private static long cash;

    @BeforeAll
    static void setUp() throws Exception {
        API.awaitReady(Duration.ofMinutes(1));
        cash = API.cashPaymentTypeId();
    }

    private static String tag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, Object> deposit(long amount) {
        return dated("transactionDate", "2026-01-05", "transactionAmount", amount, "paymentTypeId", cash);
    }

    private static Response postDeposit(long account, Map<String, Object> body, String key) throws Exception {
        return API.post("/savingsaccounts/" + account + "/transactions?command=deposit", body, key);
    }

    /** Transactions de dépôt non annulées du compte d'épargne. */
    private static List<JsonNode> deposits(long account) throws Exception {
        JsonNode acc = API.ok(API.get("/savingsaccounts/" + account + "?associations=transactions"), "savings");
        List<JsonNode> out = new ArrayList<>();
        for (JsonNode t : acc.path("transactions")) {
            if (t.path("transactionType").path("deposit").asBoolean() && !t.path("reversed").asBoolean()) {
                out.add(t);
            }
        }
        return out;
    }

    private static long balance(long account) throws Exception {
        return API.ok(API.get("/savingsaccounts/" + account), "savings")
                .path("summary")
                .path("accountBalance")
                .decimalValue()
                .longValueExact();
    }

    private static int journalLines(String filter) throws Exception {
        return API.ok(API.get("/journalentries?" + filter + "&limit=1000"), "journalentries")
                .path("totalFilteredRecords")
                .asInt();
    }

    @Test
    @Order(1)
    void a_sameSavingsDepositTwiceWithSameKey() throws Exception {
        long account = API.newSavingsAccount(tag());
        String key = newKey();

        Response first = postDeposit(account, deposit(10_000), key);
        Response second = postDeposit(account, deposit(10_000), key);
        System.out.println("(a) 1er envoi : " + first + "\n(a) 2e envoi  : " + second);

        assertEquals(200, first.status());
        assertFalse(first.servedFromCache());
        assertEquals(200, second.status());
        assertTrue(second.servedFromCache(), "le rejeu doit venir du cache");
        assertEquals(first.body(), second.body(), "même corps de réponse, même resourceId");
        assertEquals(1, deposits(account).size());
        assertEquals(10_000, balance(account));
        assertEquals(2, journalLines("savingsId=" + account));
    }

    @Test
    @Order(2)
    void b_sameLoanRepaymentTwice() throws Exception {
        long loan = API.newDisbursedLoan(tag());
        String key = newKey();
        Map<String, Object> body =
                dated("transactionDate", "2026-02-02", "transactionAmount", 20_000, "paymentTypeId", cash);

        Response first = API.post("/loans/" + loan + "/transactions?command=repayment", body, key);
        Response second = API.post("/loans/" + loan + "/transactions?command=repayment", body, key);
        System.out.println("(b) 1er envoi : " + first + "\n(b) 2e envoi  : " + second);

        assertEquals(200, first.status());
        assertEquals(200, second.status());
        assertTrue(second.servedFromCache());
        assertEquals(first.body(), second.body());
        int repayments = 0;
        JsonNode l = API.ok(API.get("/loans/" + loan + "?associations=transactions"), "loan");
        for (JsonNode t : l.path("transactions")) {
            if (t.path("type").path("repayment").asBoolean() && !t.path("manuallyReversed").asBoolean()) {
                repayments++;
            }
        }
        assertEquals(1, repayments);
        assertEquals(20_000, l.path("summary").path("totalRepayment").decimalValue().longValueExact());
    }

    @Test
    @Order(3)
    void c_concurrentSendsWithSameKey() throws Exception {
        int rounds = 10;
        int parallel = 4;
        List<String> observed = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(parallel);
        try {
            for (int round = 0; round < rounds; round++) {
                long account = API.newSavingsAccount(tag());
                String key = newKey();
                CountDownLatch start = new CountDownLatch(1);
                List<Future<Response>> futures = new ArrayList<>();
                for (int i = 0; i < parallel; i++) {
                    Callable<Response> call = () -> {
                        start.await();
                        return postDeposit(account, deposit(5_000), key);
                    };
                    futures.add(pool.submit(call));
                }
                start.countDown();
                List<String> statuses = new ArrayList<>();
                int fresh = 0;
                for (Future<Response> f : futures) {
                    Response r = f.get(90, TimeUnit.SECONDS);
                    statuses.add(r.status() + (r.servedFromCache() ? "(cache)" : ""));
                    if (r.status() == 200 && !r.servedFromCache()) {
                        fresh++;
                    }
                    if (r.status() != 200) {
                        System.out.println("(c) réponse non 200 : " + r);
                    }
                }
                observed.add(statuses.toString());
                assertEquals(1, deposits(account).size(), "une seule transaction, tour " + round + " " + statuses);
                assertEquals(5_000, balance(account));
                assertEquals(1, fresh, "un seul envoi réellement traité, tour " + round + " " + statuses);
            }
        } finally {
            pool.shutdownNow();
        }
        System.out.println("(c) statuts par tour (" + parallel + " envois simultanés) : " + observed);
    }

    @Test
    @Order(4)
    void d_sameKeyDifferentBody() throws Exception {
        long account = API.newSavingsAccount(tag());
        String key = newKey();

        Response first = postDeposit(account, deposit(10_000), key);
        Response second = postDeposit(account, deposit(25_000), key);
        System.out.println("(d) 1er envoi (10 000) : " + first + "\n(d) 2e envoi (25 000)  : " + second);

        assertEquals(200, first.status());
        // Fineract ne compare pas les corps : la seconde demande est ignorée et reçoit la réponse de la première.
        assertEquals(200, second.status());
        assertTrue(second.servedFromCache());
        assertEquals(first.body(), second.body());
        assertEquals(1, deposits(account).size());
        assertEquals(10_000, balance(account));
    }

    @Test
    @Order(5)
    void d2_sameKeyOnAnotherAccountIsSilentlyDropped() throws Exception {
        long accountA = API.newSavingsAccount(tag());
        long accountB = API.newSavingsAccount(tag());
        String key = newKey();

        Response first = postDeposit(accountA, deposit(10_000), key);
        Response second = postDeposit(accountB, deposit(10_000), key);
        System.out.println("(d2) compte A : " + first + "\n(d2) compte B : " + second);

        // La clé est unique par (action, entité) et non par compte : le dépôt sur B n'est jamais passé.
        assertEquals(200, second.status());
        assertTrue(second.servedFromCache());
        assertEquals(accountA, second.body().path("savingsId").asLong());
        assertEquals(1, deposits(accountA).size());
        assertEquals(0, deposits(accountB).size());
    }

    @Test
    @Order(6)
    void f_failedCommandIsCachedUnderItsKey() throws Exception {
        long account = API.newSavingsAccount(tag());
        String key = newKey();
        // Date future : refus de validation.
        Map<String, Object> bad = dated("transactionDate", "2099-01-05", "transactionAmount", 10_000, "paymentTypeId", cash);

        Response first = postDeposit(account, bad, key);
        Response corrected = postDeposit(account, deposit(10_000), key);
        Response newKey = postDeposit(account, deposit(10_000), newKey());
        System.out.println("(f) envoi refusé       : " + first + "\n(f) corrigé, même clé  : " + corrected
                + "\n(f) corrigé, clé neuve : " + newKey);

        assertEquals(400, first.status());
        assertFalse(first.servedFromCache());
        assertEquals(400, corrected.status(), "l'échec reste attaché à la clé");
        assertTrue(corrected.servedFromCache());
        assertEquals(200, newKey.status());
        assertEquals(1, deposits(account).size());
    }

    @Test
    @Order(7)
    void g_noKeyMeansNoProtection() throws Exception {
        long account = API.newSavingsAccount(tag());

        Response first = postDeposit(account, deposit(10_000), null);
        Response second = postDeposit(account, deposit(10_000), null);
        System.out.println("(g) sans clé : " + first + " / " + second);

        assertEquals(200, second.status());
        assertFalse(second.servedFromCache());
        assertEquals(2, deposits(account).size(), "sans clé, Fineract génère une clé par envoi : doublon");
    }

    @Test
    @Order(8)
    void h_transactionExternalIdIsUniqueEvenWithNewKey() throws Exception {
        long account = API.newSavingsAccount(tag());
        long loan = API.newDisbursedLoan(tag());
        String savingsExt = "SPIKE-OP-" + UUID.randomUUID();
        String loanExt = "SPIKE-OP-" + UUID.randomUUID();
        Map<String, Object> dep = deposit(10_000);
        dep.put("externalId", savingsExt);
        Map<String, Object> rep =
                dated("transactionDate", "2026-02-02", "transactionAmount", 20_000, "paymentTypeId", cash, "externalId", loanExt);

        Response dep1 = postDeposit(account, dep, newKey());
        Response dep2 = postDeposit(account, dep, newKey());
        Response rep1 = API.post("/loans/" + loan + "/transactions?command=repayment", rep, newKey());
        Response rep2 = API.post("/loans/" + loan + "/transactions?command=repayment", rep, newKey());
        System.out.println("(h) dépôt, clé neuve, même externalId : " + dep2 + "\n(h) remboursement, idem : " + rep2);

        assertEquals(200, dep1.status());
        assertEquals(403, dep2.status(), "contrainte uk_m_sat_external_id");
        assertEquals("error.msg.data.integrity.issue", dep2.body().path("userMessageGlobalisationCode").asText());
        assertEquals(1, deposits(account).size());
        assertEquals(savingsExt,
                API.ok(API.get("/savingsaccounts/" + account + "/transactions/external-id/" + savingsExt), "tx")
                        .path("externalId")
                        .asText());
        assertEquals(200, rep1.status());
        assertEquals(loanExt, rep1.body().path("resourceExternalId").asText());
        assertEquals(400, rep2.status());
        assertTrue(rep2.raw().contains("value.must.be.unique"), rep2.raw());
    }

    @Test
    @Order(9)
    void e_replayAfterFineractRestart() throws Exception {
        long account = API.newSavingsAccount(tag());
        String key = newKey();
        Response first = postDeposit(account, deposit(10_000), key);
        assertEquals(200, first.status());

        restartFineract();

        Response replay = postDeposit(account, deposit(10_000), key);
        System.out.println("(e) avant redémarrage : " + first + "\n(e) après redémarrage : " + replay);
        assertEquals(200, replay.status());
        assertTrue(replay.servedFromCache());
        assertEquals(first.body(), replay.body());
        assertEquals(1, deposits(account).size());
        assertEquals(10_000, balance(account));
    }

    private static void restartFineract() throws IOException, InterruptedException {
        String compose = System.getProperty("nkulkap.compose.file", "../../deploy/compose/dev.yml");
        Process p = new ProcessBuilder("docker", "compose", "-f", compose, "restart", "fineract")
                .inheritIO()
                .start();
        if (!p.waitFor(3, TimeUnit.MINUTES) || p.exitValue() != 0) {
            throw new IllegalStateException("docker compose restart fineract a échoué");
        }
        long t0 = System.nanoTime();
        API.awaitReady(Duration.ofMinutes(8));
        System.out.println("(e) Fineract de nouveau disponible après " + Duration.ofNanos(System.nanoTime() - t0).toSeconds() + " s");
    }
}
