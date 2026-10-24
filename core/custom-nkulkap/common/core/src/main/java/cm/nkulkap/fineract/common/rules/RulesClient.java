/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.rules;

import cm.nkulkap.fineract.common.error.NkulkapErrorCode;
import cm.nkulkap.fineract.common.error.NkulkapException;
import cm.nkulkap.fineract.common.journal.Posting;
import cm.nkulkap.fineract.common.money.Currency;
import cm.nkulkap.fineract.common.money.Money;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client de nkulkap-rules (FR-L3-01) : {@code POST /v1/evaluate}. L'évaluation est pure, donc rejouable sans risque.
 *
 * <p>Politique réseau : délai de requête borné ; nouvelles tentatives sur erreur d'entrée-sortie, délai dépassé et
 * HTTP 502/503/504 avec attente exponentielle (200, 400, 800 ms par défaut) ; HTTP 4xx = rejet définitif ;
 * tentatives épuisées = {@link NkulkapErrorCode#RULES_UNAVAILABLE}.
 */
public final class RulesClient {

    public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(5);
    public static final int DEFAULT_MAX_RETRIES = 3;
    public static final Duration DEFAULT_INITIAL_BACKOFF = Duration.ofMillis(200);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final URI evaluateUri;
    private final HttpClient httpClient;
    private final Duration requestTimeout;
    private final int maxRetries;
    private final Duration initialBackoff;

    /** Client aux paramètres par défaut. */
    public RulesClient(URI baseUri) {
        this(
                baseUri,
                HttpClient.newBuilder().connectTimeout(DEFAULT_CONNECT_TIMEOUT).build(),
                DEFAULT_REQUEST_TIMEOUT,
                DEFAULT_MAX_RETRIES,
                DEFAULT_INITIAL_BACKOFF);
    }

    /**
     * @param httpClient client dont le délai de connexion est réglé par l'appelant
     * @param maxRetries nombre de nouvelles tentatives après la première (3 au plus)
     * @param initialBackoff attente avant la première nouvelle tentative, doublée à chaque fois
     */
    public RulesClient(
            URI baseUri, HttpClient httpClient, Duration requestTimeout, int maxRetries, Duration initialBackoff) {
        Objects.requireNonNull(baseUri, "baseUri");
        String base = baseUri.toString();
        this.evaluateUri =
                URI.create((base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/v1/evaluate");
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout");
        if (maxRetries < 0 || maxRetries > 3) {
            throw new IllegalArgumentException("maxRetries must be between 0 and 3");
        }
        this.maxRetries = maxRetries;
        this.initialBackoff = Objects.requireNonNull(initialBackoff, "initialBackoff");
    }

    public RulesResult evaluate(String tenant, String ruleId, String ruleVersion, Map<String, String> variables) {
        HttpRequest request = HttpRequest.newBuilder(evaluateUri)
                .timeout(requestTimeout)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body(tenant, ruleId, ruleVersion, variables)))
                .build();
        Throwable lastFailure = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                pause(initialBackoff.multipliedBy(1L << (attempt - 1)));
            }
            try {
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = response.statusCode();
                if (status == 200) {
                    return parse(response.body());
                }
                if (status == 502 || status == 503 || status == 504) {
                    lastFailure = new IOException("HTTP " + status);
                    continue;
                }
                throw new NkulkapException(
                        NkulkapErrorCode.RULES_REJECTED,
                        "nkulkap-rules answered HTTP " + status + ": " + response.body(),
                        status,
                        response.body());
            } catch (IOException e) {
                // Inclut HttpTimeoutException : service injoignable ou trop lent, on réessaie.
                lastFailure = e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new NkulkapException(
                        NkulkapErrorCode.RULES_UNAVAILABLE, "Interrupted while calling nkulkap-rules", e);
            }
        }
        throw new NkulkapException(
                NkulkapErrorCode.RULES_UNAVAILABLE,
                "nkulkap-rules unavailable after " + (maxRetries + 1) + " attempts: " + lastFailure,
                lastFailure);
    }

    private static String body(String tenant, String ruleId, String ruleVersion, Map<String, String> variables) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("tenant", tenant);
        root.put("rule_id", ruleId);
        root.put("rule_version", ruleVersion);
        ObjectNode vars = root.putObject("variables");
        variables.forEach(vars::put);
        return root.toString();
    }

    private void pause(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NkulkapException(NkulkapErrorCode.RULES_UNAVAILABLE, "Interrupted while waiting to retry", e);
        }
    }

    private static RulesResult parse(String json) {
        try {
            JsonNode root = MAPPER.readTree(json);
            JsonNode postingsNode = root.path("postings");
            JsonNode sha = root.path("rule_sha256");
            if (!postingsNode.isArray() || !sha.isTextual()) {
                throw invalid("missing postings or rule_sha256", null);
            }
            List<Posting> postings = new ArrayList<>();
            for (JsonNode p : postingsNode) {
                postings.add(posting(p));
            }
            return new RulesResult(postings, sha.asText());
        } catch (IOException e) {
            throw invalid("unreadable JSON", e);
        }
    }

    private static Posting posting(JsonNode p) {
        JsonNode source = p.path("source");
        JsonNode destination = p.path("destination");
        JsonNode amount = p.path("amount");
        if (!source.isTextual() || !destination.isTextual()) {
            throw invalid("posting without source or destination", null);
        }
        if (!"XAF".equals(p.path("asset").asText(null))) {
            throw invalid("unknown asset: " + p.path("asset"), null);
        }
        if (!amount.isIntegralNumber() || !amount.canConvertToLong() || amount.asLong() <= 0) {
            throw invalid("amount must be a positive whole number: " + amount, null);
        }
        return new Posting(source.asText(), destination.asText(), Money.of(amount.asLong(), Currency.XAF));
    }

    private static NkulkapException invalid(String detail, Throwable cause) {
        return new NkulkapException(
                NkulkapErrorCode.RULES_INVALID_RESPONSE, "Invalid nkulkap-rules response: " + detail, cause);
    }
}
