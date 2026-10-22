/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 */
package cm.nkulkap.spikes;

import static cm.nkulkap.spikes.FineractApi.dated;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cm.nkulkap.spikes.FineractApi.Response;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Étape 1.8 : commande {@code force-withdrawal} sur un compte d'épargne. Fige docs/specs/force-withdrawal.md. Exige la
 * pile locale et le seed (make up && make seed). Chaque test remet la configuration globale à son état par défaut.
 */
class ForceWithdrawalSpikeIT {

    private static final String CONFIG = "allow-force-withdrawal-on-savings-account";
    private static final FineractApi API = new FineractApi();
    private static long cash;
    private static long configId;

    @BeforeAll
    static void setUp() throws Exception {
        API.awaitReady(Duration.ofMinutes(1));
        cash = API.cashPaymentTypeId();
        configId = API.ok(API.get("/configurations/name/" + CONFIG), CONFIG).path("id").asLong();
    }

    @AfterEach
    void restoreDefaultConfiguration() throws Exception {
        setConfig(false, 0);
    }

    private static void setConfig(boolean enabled, long limit) throws Exception {
        API.ok(API.send("PUT", "/configurations/" + configId, Map.of("enabled", enabled, "value", limit), null), "config");
    }

    private static String tag() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static Map<String, Object> amount(long xaf) {
        return dated("transactionDate", "2026-02-01", "transactionAmount", xaf, "paymentTypeId", cash);
    }

    private static Response force(FineractApi api, long account, long xaf) throws Exception {
        return api.post("/savingsaccounts/" + account + "/transactions?command=force-withdrawal", amount(xaf), null);
    }

    private static long fundedAccount(long xaf) throws Exception {
        long account = API.newSavingsAccount(tag());
        API.ok(API.post("/savingsaccounts/" + account + "/transactions?command=deposit", amount(xaf), null), "deposit");
        return account;
    }

    private static long balance(long account) throws Exception {
        return API.ok(API.get("/savingsaccounts/" + account), "savings")
                .path("summary")
                .path("accountBalance")
                .decimalValue()
                .longValueExact();
    }

    private static String insufficientBalance(Response r) {
        return r.body().path("errors").path(0).path("userMessageGlobalisationCode").asText();
    }

    /** Écritures de la transaction d'épargne, sous la forme « code GL : DEBIT|CREDIT montant ». */
    private static Map<String, Long> journal(long transactionId) throws Exception {
        Map<String, Long> out = new HashMap<>();
        for (JsonNode e : API.ok(API.get("/journalentries?transactionId=S" + transactionId), "journal").path("pageItems")) {
            out.merge(e.path("glAccountCode").asText() + " " + e.path("entryType").path("value").asText(),
                    e.path("amount").decimalValue().longValueExact(), Long::sum);
        }
        return out;
    }

    /** Mot de passe FICTIF conforme à la politique de Fineract : 12 à 50 caractères, sans répétition consécutive. */
    private static String testPassword() {
        StringBuilder sb = new StringBuilder("Fk!");
        for (char c : UUID.randomUUID().toString().replace("-", "").toCharArray()) {
            if (c != sb.charAt(sb.length() - 1)) {
                sb.append(c);
            }
        }
        return sb.append("A1#").toString();
    }

    @Test
    void disabledByDefault_actsAsAPlainWithdrawal() throws Exception {
        long account = fundedAccount(40_000);

        Response over = force(API, account, 60_000);
        Response within = force(API, account, 10_000);
        System.out.println("(défaut) au-delà du solde : " + over + "\n(défaut) dans le solde : " + within);

        assertEquals(403, over.status());
        assertEquals("error.msg.savingsaccount.transaction.insufficient.account.balance", insufficientBalance(over));
        assertEquals(200, within.status());
        assertEquals(30_000, balance(account));
        assertEquals(Map.of("FICTIF-3711 DEBIT", 10_000L, "FICTIF-5711 CREDIT", 10_000L), journal(within.resourceId()));
    }

    @Test
    void enabled_allowsNegativeBalanceUpToTheLimit() throws Exception {
        setConfig(true, 50_000);
        long account = fundedAccount(40_000);

        Response first = force(API, account, 60_000);
        System.out.println("(activé, plafond 50 000) retrait forcé de 60 000 sur 40 000 : " + first);
        assertEquals(200, first.status());
        assertEquals(-20_000, balance(account));
        JsonNode tx = API.ok(API.get("/savingsaccounts/" + account + "/transactions/" + first.resourceId()), "tx");
        assertEquals("Withdrawal", tx.path("transactionType").path("value").asText(), "type de transaction : retrait ordinaire");
        // Part couverte : dépôts de la clientèle ; part à découvert : compte de découvert (overdraftPortfolioControlId).
        assertEquals(
                Map.of("FICTIF-3711 DEBIT", 40_000L, "FICTIF-2031 DEBIT", 20_000L, "FICTIF-5711 CREDIT", 60_000L),
                journal(first.resourceId()));

        Response beyond = force(API, account, 40_000);
        Response plain = API.post("/savingsaccounts/" + account + "/transactions?command=withdrawal", amount(100), null);
        System.out.println("(activé) au-delà du plafond : " + beyond + "\n(activé) retrait ordinaire à découvert : " + plain);
        assertEquals(403, beyond.status(), "solde -60 000 < -50 000");
        assertEquals("error.msg.savingsaccount.transaction.insufficient.account.balance", insufficientBalance(beyond));
        assertEquals(403, plain.status(), "le retrait ordinaire reste interdit à découvert");
        assertEquals(-20_000, balance(account));

        Response exactLimit = force(API, account, 30_000);
        assertEquals(200, exactLimit.status(), "solde -50 000 = -plafond : accepté");
        assertEquals(-50_000, balance(account));
    }

    @Test
    void requiresItsOwnPermission() throws Exception {
        setConfig(true, 50_000);
        long account = fundedAccount(40_000);
        // Rôle et utilisateur FICTIFS, pile locale uniquement : retrait ordinaire permis, retrait forcé non.
        String name = "SPIKE-FW-" + tag();
        long role = API.ok(API.post("/roles", Map.of("name", name, "description", "FICTIF étape 1.8"), null), "role")
                .path("resourceId")
                .asLong();
        API.ok(API.send("PUT", "/roles/" + role + "/permissions",
                        Map.of("permissions", Map.of("READ_SAVINGSACCOUNT", true, "WITHDRAWAL_SAVINGSACCOUNT", true)), null),
                "permissions");
        String password = testPassword();
        Map<String, Object> user = new HashMap<>();
        user.put("username", name.toLowerCase());
        user.put("firstname", "Sonde");
        user.put("lastname", "FICTIF");
        user.put("email", name.toLowerCase() + "@example.invalid");
        user.put("officeId", 1);
        user.put("roles", List.of(role));
        user.put("sendPasswordToEmail", false);
        user.put("passwordNeverExpires", true);
        user.put("password", password);
        user.put("repeatPassword", password);
        API.ok(API.post("/users", user, null), "user");
        FineractApi teller = new FineractApi(name.toLowerCase(), password);

        Response denied = force(teller, account, 10_000);
        Response plain = teller.post("/savingsaccounts/" + account + "/transactions?command=withdrawal", amount(10_000), null);
        System.out.println("(permission) retrait forcé sans FORCE_WITHDRAWAL_SAVINGSACCOUNT : " + denied
                + "\n(permission) retrait ordinaire : " + plain);

        assertEquals(403, denied.status());
        assertTrue(denied.raw().contains("FORCE_WITHDRAWAL_SAVINGSACCOUNT"), denied.raw());
        assertEquals(200, plain.status());
        assertEquals(30_000, balance(account));
    }
}
