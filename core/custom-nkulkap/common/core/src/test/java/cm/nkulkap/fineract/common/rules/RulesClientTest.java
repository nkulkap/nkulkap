/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.nkulkap.fineract.common.error.NkulkapErrorCode;
import cm.nkulkap.fineract.common.error.NkulkapException;
import cm.nkulkap.fineract.common.journal.Posting;
import cm.nkulkap.fineract.common.money.Money;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

class RulesClientTest {

    private static final String OK =
            "{\"postings\":[{\"source\":\"@client\",\"destination\":\"@frais\",\"amount\":162,\"asset\":\"XAF\"},"
                    + "{\"source\":\"@client\",\"destination\":\"@taxe\",\"amount\":252,\"asset\":\"XAF\"},"
                    + "{\"source\":\"@client\",\"destination\":\"@assurance\",\"amount\":586,\"asset\":\"XAF\"}],"
                    + "\"rule_sha256\":\"abc123\"}";

    private HttpServer server;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile String lastBody;

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    /** Démarre un serveur dont le statut et le corps dépendent du numéro d'appel (à partir de 1). */
    private URI start(IntFunction<Integer> statusFor, IntFunction<String> bodyFor, long delayMillis)
            throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/v1/evaluate", ex -> {
            int n = calls.incrementAndGet();
            lastBody = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            if (delayMillis > 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] out = bodyFor.apply(n).getBytes(StandardCharsets.UTF_8);
            try {
                ex.sendResponseHeaders(statusFor.apply(n), out.length);
                ex.getResponseBody().write(out);
            } catch (IOException ignored) {
                // Le client a abandonné (délai dépassé).
            }
            ex.close();
        });
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    private static RulesClient client(URI uri, Duration requestTimeout, int retries) {
        return new RulesClient(
                uri,
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build(),
                requestTimeout,
                retries,
                Duration.ofMillis(5));
    }

    private static RulesResult call(RulesClient c) {
        return c.evaluate("t1", "teg", "1", Map.of("montant", "100000"));
    }

    @Test
    void returnsPostings() throws IOException {
        RulesClient c = client(start(n -> 200, n -> OK, 0), Duration.ofSeconds(2), 3);
        RulesResult r = call(c);
        assertEquals(3, r.postings().size());
        assertEquals(
                new Posting("@client", "@frais", Money.xaf(162)), r.postings().get(0));
        assertEquals(Money.xaf(252), r.postings().get(1).amount());
        assertEquals(Money.xaf(586), r.postings().get(2).amount());
        assertEquals("abc123", r.ruleSha256());
        assertEquals(1, calls.get());
        assertTrue(lastBody.contains("\"rule_id\":\"teg\"") && lastBody.contains("\"montant\":\"100000\""));
    }

    @Test
    void retriesAfter503() throws IOException {
        RulesClient c = client(start(n -> n == 1 ? 503 : 200, n -> n == 1 ? "" : OK, 0), Duration.ofSeconds(2), 3);
        assertEquals(3, call(c).postings().size());
        assertEquals(2, calls.get());
    }

    @Test
    void givesUpAfterRetries() throws IOException {
        RulesClient c = client(start(n -> 503, n -> "", 0), Duration.ofSeconds(2), 3);
        NkulkapException e = assertThrows(NkulkapException.class, () -> call(c));
        assertEquals(NkulkapErrorCode.RULES_UNAVAILABLE, e.code());
        assertEquals(4, calls.get());
    }

    @Test
    void rejects4xxWithoutRetry() throws IOException {
        RulesClient c = client(start(n -> 400, n -> "bad variables", 0), Duration.ofSeconds(2), 3);
        NkulkapException e = assertThrows(NkulkapException.class, () -> call(c));
        assertEquals(NkulkapErrorCode.RULES_REJECTED, e.code());
        assertTrue(e.getMessage().contains("bad variables"));
        assertEquals(1, calls.get());
    }

    @Test
    void rejectsInvalidResponses() throws IOException {
        String[] bodies = {
            "{\"postings\":[{\"source\":\"a\",\"destination\":\"b\",\"amount\":1.5,\"asset\":\"XAF\"}],\"rule_sha256\":\"x\"}",
            "{\"postings\":[{\"source\":\"a\",\"destination\":\"b\",\"amount\":10,\"asset\":\"EUR\"}],\"rule_sha256\":\"x\"}",
            "{\"postings\":[{\"source\":\"a\",\"destination\":\"b\",\"amount\":0,\"asset\":\"XAF\"}],\"rule_sha256\":\"x\"}",
            "{\"postings\":[{\"destination\":\"b\",\"amount\":5,\"asset\":\"XAF\"}],\"rule_sha256\":\"x\"}",
            "{\"postings\":[]}",
            "pas du json"
        };
        for (String body : bodies) {
            calls.set(0);
            RulesClient c = client(start(n -> 200, n -> body, 0), Duration.ofSeconds(2), 3);
            NkulkapException e = assertThrows(NkulkapException.class, () -> call(c), body);
            assertEquals(NkulkapErrorCode.RULES_INVALID_RESPONSE, e.code(), body);
            assertEquals(1, calls.get());
            server.stop(0);
        }
    }

    @Test
    void timesOutThenUnavailable() throws IOException {
        RulesClient c = client(start(n -> 200, n -> OK, 400), Duration.ofMillis(50), 1);
        NkulkapException e = assertThrows(NkulkapException.class, () -> call(c));
        assertEquals(NkulkapErrorCode.RULES_UNAVAILABLE, e.code());
    }

    @Test
    void unreachableServerIsUnavailable() throws IOException {
        URI uri = start(n -> 200, n -> OK, 0);
        server.stop(0);
        NkulkapException e = assertThrows(NkulkapException.class, () -> call(client(uri, Duration.ofSeconds(1), 1)));
        assertEquals(NkulkapErrorCode.RULES_UNAVAILABLE, e.code());
    }

    @Test
    void validatesConstruction() {
        URI u = URI.create("http://localhost:1/");
        HttpClient http = HttpClient.newHttpClient();
        assertThrows(
                IllegalArgumentException.class,
                () -> new RulesClient(u, http, Duration.ofSeconds(1), 4, Duration.ZERO));
        assertThrows(
                IllegalArgumentException.class,
                () -> new RulesClient(u, http, Duration.ofSeconds(1), -1, Duration.ZERO));
        assertEquals(3, RulesClient.DEFAULT_MAX_RETRIES);
    }

    @Test
    void defaultClientFollowsTheDefaultPolicyAndAcceptsATrailingSlash() throws IOException {
        URI base = start(n -> 200, n -> OK, 0);
        RulesClient c = new RulesClient(URI.create(base + "/"));
        assertEquals(3, call(c).postings().size());
        assertEquals(1, calls.get());
    }

    @Test
    void interruptedCallIsUnavailableAndKeepsTheInterruptFlag() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenThrow(new InterruptedException("fictif"));
        RulesClient c =
                new RulesClient(URI.create("http://localhost:1"), http, Duration.ofSeconds(1), 3, Duration.ZERO);
        try {
            NkulkapException e = assertThrows(NkulkapException.class, () -> call(c));
            assertEquals(NkulkapErrorCode.RULES_UNAVAILABLE, e.code());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(http, times(1)).send(any(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void interruptionWhileWaitingToRetryStopsTheRetries() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(inv -> {
                    Thread.currentThread().interrupt();
                    throw new IOException("reseau perdu");
                });
        RulesClient c =
                new RulesClient(URI.create("http://localhost:1"), http, Duration.ofSeconds(1), 3, Duration.ofMillis(5));
        try {
            NkulkapException e = assertThrows(NkulkapException.class, () -> call(c));
            assertEquals(NkulkapErrorCode.RULES_UNAVAILABLE, e.code());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(http, times(1)).send(any(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        } finally {
            Thread.interrupted();
        }
    }
}
