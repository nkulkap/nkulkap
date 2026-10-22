/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 */
package cm.nkulkap.spikes;

import static cm.nkulkap.spikes.FineractApi.dated;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Étape 1.3 : événements externes de Fineract sur Kafka (topic {@code external-events}), décodés avec les classes
 * Avro de {@code fineract-avro-schemas}. Fige docs/specs/fineract-events.md. Exige la pile, le seed (qui active les
 * événements) et le jar des schémas Avro.
 *
 * <p>Réseau : Kafka sur {@code localhost:19092} (écouteur HOST de deploy/compose/dev.yml), lecture par lots de 1 s,
 * attente de chaque événement bornée à 3 min.
 */
class ExternalEventsSpikeIT {

    static final List<String> NKULKAP_EVENTS = List.of(
            "ClientCreateBusinessEvent",
            "ClientActivateBusinessEvent",
            "SavingsDepositBusinessEvent",
            "SavingsWithdrawalBusinessEvent",
            "SavingsAccountForceWithdrawalBusinessEvent",
            "LoanDisbursalTransactionBusinessEvent",
            "LoanTransactionMakeRepaymentPostBusinessEvent",
            "LoanBalanceChangedBusinessEvent");

    private static final FineractApi API = new FineractApi();
    private static final Duration MAX_WAIT = Duration.ofMinutes(3);
    private static KafkaConsumer<byte[], byte[]> consumer;
    private static Method messageFromBytes;
    private static final List<Received> received = new ArrayList<>();

    /** Événement reçu : enveloppe MessageV1, charge utile décodée, instant de réception. */
    record Received(SpecificRecord envelope, SpecificRecord payload, long receivedAtMillis) {

        String type() {
            return String.valueOf(get(envelope, "type"));
        }
    }

    @BeforeAll
    static void setUp() throws Exception {
        try {
            messageFromBytes = Class.forName("org.apache.fineract.avro.MessageV1").getMethod("fromByteBuffer", ByteBuffer.class);
        } catch (ClassNotFoundException e) {
            assumeTrue(false, "jar fineract-avro-schemas absent : construire l'image Fineract");
        }
        API.awaitReady(Duration.ofMinutes(1));
        Map<String, Boolean> enable = new LinkedHashMap<>();
        NKULKAP_EVENTS.forEach(t -> enable.put(t, true));
        API.ok(API.send("PUT", "/externalevents/configuration", Map.of("externalEventConfigurations", enable), null),
                "externalevents");

        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("nkulkap.kafka", "localhost:19092"));
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "nkulkap-spike-" + UUID.randomUUID());
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        p.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "15000");
        p.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "30000");
        consumer = new KafkaConsumer<>(p, new ByteArrayDeserializer(), new ByteArrayDeserializer());
        List<TopicPartition> partitions = consumer.partitionsFor("external-events", Duration.ofSeconds(30)).stream()
                .map(i -> new TopicPartition(i.topic(), i.partition()))
                .toList();
        consumer.assign(partitions);
        consumer.seekToEnd(partitions);
        partitions.forEach(consumer::position); // fige la position avant les actions
    }

    @AfterAll
    static void tearDown() {
        if (consumer != null) {
            consumer.close(Duration.ofSeconds(5));
        }
    }

    private static Object get(SpecificRecord r, String field) {
        return r.get(r.getSchema().getField(field).pos());
    }

    private static Received await(String type, Predicate<SpecificRecord> match) throws Exception {
        long deadline = System.currentTimeMillis() + MAX_WAIT.toMillis();
        while (true) {
            for (Received r : received) {
                if (r.type().equals(type) && match.test(r.payload())) {
                    return r;
                }
            }
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("événement " + type + " non reçu en " + MAX_WAIT);
            }
            for (ConsumerRecord<byte[], byte[]> rec : consumer.poll(Duration.ofSeconds(1))) {
                SpecificRecord env = (SpecificRecord) messageFromBytes.invoke(null, ByteBuffer.wrap(rec.value()));
                String schema = String.valueOf(get(env, "dataschema"));
                ByteBuffer data = (ByteBuffer) get(env, "data");
                SpecificRecord payload = (SpecificRecord)
                        Class.forName(schema).getMethod("fromByteBuffer", ByteBuffer.class).invoke(null, data);
                received.add(new Received(env, payload, System.currentTimeMillis()));
            }
        }
    }

    private static void report(String action, long sentAtMillis, Received r) {
        SpecificRecord e = r.envelope();
        System.out.printf("(%s) %s | schéma %s | catégorie %s | createdAt %s | businessDate %s | idempotencyKey %s | "
                        + "latence après réponse HTTP : %d ms%n  charge utile : %s%n",
                action, r.type(), get(e, "dataschema"), get(e, "category"), get(e, "createdAt"), get(e, "businessDate"),
                get(e, "idempotencyKey"), r.receivedAtMillis() - sentAtMillis,
                String.valueOf(r.payload()).substring(0, Math.min(1500, String.valueOf(r.payload()).length())));
    }

    private static long amount(SpecificRecord r) {
        return ((BigDecimal) get(r, "amount")).longValueExact();
    }

    @Test
    void depositEventIsPublishedAndDecoded() throws Exception {
        long cash = API.cashPaymentTypeId();
        String tag = UUID.randomUUID().toString().substring(0, 8);

        long t0 = System.currentTimeMillis();
        long account = API.newSavingsAccount(tag);
        Received clientCreated = await("ClientCreateBusinessEvent",
                r -> String.valueOf(get(r, "externalId")).equals("SPIKE-CLI-" + tag));
        report("client", t0, clientCreated);

        long t1 = System.currentTimeMillis();
        String opId = "SPIKE-OP-" + UUID.randomUUID();
        Map<String, Object> body =
                dated("transactionDate", "2026-02-01", "transactionAmount", 12_345, "paymentTypeId", cash, "externalId", opId);
        long txId = API.ok(API.post("/savingsaccounts/" + account + "/transactions?command=deposit", body, opId), "deposit")
                .path("resourceId")
                .asLong();
        long t1done = System.currentTimeMillis();
        Received deposit = await("SavingsDepositBusinessEvent", r -> ((Long) get(r, "accountId")) == account);
        report("dépôt", t1done, deposit);
        assertEquals("org.apache.fineract.avro.savings.v1.SavingsAccountTransactionDataV1",
                String.valueOf(get(deposit.envelope(), "dataschema")));
        assertEquals(txId, (Long) get(deposit.payload(), "id"));
        assertEquals(12_345, amount(deposit.payload()));
        assertNotNull(get(deposit.envelope(), "idempotencyKey"));
        System.out.println("(dépôt) durée de l'appel HTTP : " + (t1done - t1) + " ms");

        API.ok(API.post("/savingsaccounts/" + account + "/transactions?command=withdrawal",
                        dated("transactionDate", "2026-02-01", "transactionAmount", 2_345, "paymentTypeId", cash), null),
                "withdrawal");
        report("retrait", System.currentTimeMillis(),
                await("SavingsWithdrawalBusinessEvent", r -> ((Long) get(r, "accountId")) == account));

        long t3 = System.currentTimeMillis();
        long loan = API.newDisbursedLoan(tag + "L");
        report("décaissement", t3, await("LoanDisbursalTransactionBusinessEvent", r -> ((Long) get(r, "loanId")) == loan));
        API.ok(API.post("/loans/" + loan + "/transactions?command=repayment",
                        dated("transactionDate", "2026-02-02", "transactionAmount", 20_000, "paymentTypeId", cash), null),
                "repayment");
        long t4done = System.currentTimeMillis();
        report("remboursement", t4done,
                await("LoanTransactionMakeRepaymentPostBusinessEvent", r -> ((Long) get(r, "loanId")) == loan));
        report("solde du prêt", t4done, await("LoanBalanceChangedBusinessEvent", r -> ((Long) get(r, "id")) == loan));
        System.out.println("(types reçus pendant le test) "
                + received.stream().map(Received::type).distinct().toList() + " ; total " + received.size());
    }
}
