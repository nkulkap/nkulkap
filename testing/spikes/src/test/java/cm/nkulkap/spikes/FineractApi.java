/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 */
package cm.nkulkap.spikes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Client HTTP de l'API Fineract pour les tests d'exploration.
 *
 * <p>Réseau : délai de connexion 10 s, délai de réponse 60 s ; aucune nouvelle tentative implicite (les tests observent
 * chaque envoi tel quel) ; un échec réseau lève {@link IOException}. Certificat autosigné de la pile locale accepté.
 */
public final class FineractApi {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String SERVED_FROM_CACHE = "x-served-from-cache";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    public static final ObjectMapper JSON = new ObjectMapper();

    private final String base;
    private final String auth;
    private final String tenant;
    private final HttpClient http;

    /** Réponse brute : statut, en-tête de cache, corps JSON (ou nœud texte si non JSON). */
    public record Response(int status, boolean servedFromCache, JsonNode body, String raw) {

        public long resourceId() {
            return body.path("resourceId").asLong();
        }

        @Override
        public String toString() {
            return "HTTP " + status + (servedFromCache ? " [x-served-from-cache: true] " : " ") + raw;
        }
    }

    public FineractApi() {
        // Identifiants FICTIFS de l'image de développement Fineract.
        this(prop("nkulkap.fineract.user", "mifos"), prop("nkulkap.fineract.password", "password"));
    }

    /** Client authentifié sous un autre utilisateur (utilisateurs FICTIFS créés par les tests). */
    public FineractApi(String user, String password) {
        this.base = prop("nkulkap.fineract.url", "https://localhost:8443/fineract-provider/api/v1");
        this.tenant = prop("nkulkap.fineract.tenant", "default");
        this.auth = "Basic " + Base64.getEncoder().encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .sslContext(trustAll())
                .sslParameters(noHostnameCheck())
                .build();
    }

    public Response get(String path) throws IOException, InterruptedException {
        return send("GET", path, null, null);
    }

    public Response post(String path, Object body, String idempotencyKey) throws IOException, InterruptedException {
        return send("POST", path, body, idempotencyKey);
    }

    public Response send(String method, String path, Object body, String idempotencyKey)
            throws IOException, InterruptedException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .timeout(REQUEST_TIMEOUT)
                .header("Authorization", auth)
                .header("Fineract-Platform-TenantId", tenant)
                .header("Accept", "application/json");
        if (idempotencyKey != null) {
            b.header(IDEMPOTENCY_KEY, idempotencyKey);
        }
        if (body != null) {
            b.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(JSON.writeValueAsString(body)));
        } else {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        }
        HttpResponse<String> r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        boolean cached = r.headers().firstValue(SERVED_FROM_CACHE).map("true"::equalsIgnoreCase).orElse(false);
        JsonNode node;
        try {
            node = r.body().isEmpty() ? JSON.nullNode() : JSON.readTree(r.body());
        } catch (IOException e) {
            node = JSON.getNodeFactory().textNode(r.body());
        }
        return new Response(r.statusCode(), cached, node, r.body());
    }

    /** Lève une erreur explicite si la réponse n'est pas 200. */
    public JsonNode ok(Response r, String what) {
        if (r.status() != 200) {
            throw new IllegalStateException(what + " : " + r);
        }
        return r.body();
    }

    /** Attend que Fineract réponde 200 sur /offices, jusqu'au délai donné. */
    public void awaitReady(Duration max) throws InterruptedException {
        long deadline = System.nanoTime() + max.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                if (get("/offices").status() == 200) {
                    return;
                }
            } catch (IOException e) {
                // Fineract redémarre : on réessaie.
            }
            Thread.sleep(5_000);
        }
        throw new IllegalStateException("Fineract indisponible après " + max);
    }

    // --- Fabriques de données FICTIVES -------------------------------------------------------

    public static Map<String, Object> dated(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        m.put("dateFormat", "yyyy-MM-dd");
        m.put("locale", "fr");
        return m;
    }

    public static String newKey() {
        return UUID.randomUUID().toString();
    }

    public long productId(String path, String name) throws IOException, InterruptedException {
        for (JsonNode p : ok(get(path), path)) {
            if (name.equals(p.path("name").asText())) {
                return p.path("id").asLong();
            }
        }
        throw new IllegalStateException("Produit « " + name + " » absent : lancer make seed");
    }

    public long cashPaymentTypeId() throws IOException, InterruptedException {
        for (JsonNode t : ok(get("/paymenttypes"), "paymenttypes")) {
            if (t.path("isCashPayment").asBoolean()) {
                return t.path("id").asLong();
            }
        }
        throw new IllegalStateException("Type de paiement espèces absent : lancer make seed");
    }

    /** Crée un client FICTIF actif dans l'agence siège. */
    public long newClient(String tag) throws IOException, InterruptedException {
        Map<String, Object> body = dated(
                "officeId", 1,
                "legalFormId", 1,
                "firstname", "Sonde",
                "lastname", "FICTIF-" + tag,
                "externalId", "SPIKE-CLI-" + tag,
                "active", true,
                "activationDate", "2026-01-02",
                "submittedOnDate", "2026-01-02");
        return ok(post("/clients", body, null), "client").path("resourceId").asLong();
    }

    /** Ouvre un compte d'épargne actif sur le produit du seed. */
    public long newSavingsAccount(String tag) throws IOException, InterruptedException {
        long client = newClient(tag);
        long product = productId("/savingsproducts", "FICTIF Épargne à vue");
        long id = ok(post("/savingsaccounts", dated("clientId", client, "productId", product, "submittedOnDate", "2026-01-02"), null),
                        "savingsaccount")
                .path("savingsId")
                .asLong();
        ok(post("/savingsaccounts/" + id + "?command=approve", dated("approvedOnDate", "2026-01-02"), null), "approve");
        ok(post("/savingsaccounts/" + id + "?command=activate", dated("activatedOnDate", "2026-01-02"), null), "activate");
        return id;
    }

    /** Crée, approuve et décaisse un prêt de 500 000 XAF sur le produit du seed. */
    public long newDisbursedLoan(String tag) throws IOException, InterruptedException {
        long client = newClient(tag);
        long product = productId("/loanproducts", "FICTIF Crédit ordinaire");
        Map<String, Object> body = dated(
                "clientId", client,
                "productId", product,
                "loanType", "individual",
                "principal", 500_000,
                "loanTermFrequency", 6,
                "loanTermFrequencyType", 2,
                "numberOfRepayments", 6,
                "repaymentEvery", 1,
                "repaymentFrequencyType", 2,
                "interestRatePerPeriod", 2,
                "amortizationType", 1,
                "interestType", 1,
                "interestCalculationPeriodType", 1,
                "transactionProcessingStrategyCode", "mifos-standard-strategy",
                "expectedDisbursementDate", "2026-01-02",
                "submittedOnDate", "2026-01-02");
        long id = ok(post("/loans", body, null), "loan").path("loanId").asLong();
        ok(post("/loans/" + id + "?command=approve", dated("approvedOnDate", "2026-01-02", "approvedLoanAmount", 500_000), null),
                "approve");
        ok(post("/loans/" + id + "?command=disburse",
                        dated("actualDisbursementDate", "2026-01-02", "transactionAmount", 500_000, "paymentTypeId",
                                cashPaymentTypeId()),
                        null),
                "disburse");
        return id;
    }

    private static String prop(String key, String def) {
        return System.getProperty(key, def);
    }

    private static SSLContext trustAll() {
        try {
            TrustManager tm = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] c, String a) {}

                @Override
                public void checkServerTrusted(X509Certificate[] c, String a) {}

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[] {tm}, new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static SSLParameters noHostnameCheck() {
        SSLParameters p = new SSLParameters();
        p.setEndpointIdentificationAlgorithm(null);
        return p;
    }
}
