/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.nkulkap.fineract.common.audit.AuditChain.Stored;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class AuditServiceTest {

    private static final Instant T0 = Instant.parse("2026-10-21T08:00:00Z");
    private static final UUID OP = UUID.fromString("0192f1e2-1111-7aaa-8bbb-000000000001");

    private JdbcTemplate jdbc;
    private AuditService service;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        service = new AuditService(jdbc);
    }

    private static AuditEntry entry(int i) {
        return new AuditEntry(
                T0.plusSeconds(i),
                "fictif.caissier",
                "TOURNEE_RAPPROCHER",
                "tournee",
                String.valueOf(500 + i),
                OP,
                "{\"ecart\":-500}");
    }

    @Test
    void firstRecordStartsFromGenesisAndTakesTheLockBeforeReadingTheLastHash() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of());
        AuditEntry e = entry(0);

        String hash = service.record(e);

        assertEquals(AuditChain.hash(AuditChain.GENESIS, e), hash);
        InOrder order = inOrder(jdbc);
        order.verify(jdbc).queryForObject(anyString(), eq(Object.class), eq(AuditService.LOCK_KEY));
        order.verify(jdbc).queryForList(anyString(), eq(String.class));
        order.verify(jdbc)
                .update(
                        anyString(),
                        eq(Timestamp.from(T0)),
                        eq("fictif.caissier"),
                        eq("TOURNEE_RAPPROCHER"),
                        eq("tournee"),
                        eq("500"),
                        eq(OP),
                        eq("{\"ecart\":-500}"),
                        eq(AuditChain.GENESIS),
                        eq(hash));
    }

    @Test
    void nextRecordChainsOntoTheLastStoredHash() {
        String last = AuditChain.hash(AuditChain.GENESIS, entry(0));
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of(last));
        AuditEntry e = entry(1);

        String hash = service.record(e);

        assertEquals(AuditChain.hash(last, e), hash);
        verify(jdbc)
                .update(
                        anyString(),
                        eq(Timestamp.from(T0.plusSeconds(1))),
                        eq("fictif.caissier"),
                        eq("TOURNEE_RAPPROCHER"),
                        eq("tournee"),
                        eq("501"),
                        eq(OP),
                        eq("{\"ecart\":-500}"),
                        eq(last),
                        eq(hash));
    }

    @Test
    void optionalFieldsAreWrittenAsNull() {
        when(jdbc.queryForList(anyString(), eq(String.class))).thenReturn(List.of());
        AuditEntry e = new AuditEntry(T0, "fictif.admin", "CONFIG", "regle", null, null, null);

        String hash = service.record(e);

        verify(jdbc)
                .update(
                        anyString(),
                        eq(Timestamp.from(T0)),
                        eq("fictif.admin"),
                        eq("CONFIG"),
                        eq("regle"),
                        isNull(),
                        isNull(),
                        isNull(),
                        eq(AuditChain.GENESIS),
                        eq(hash));
    }

    @Test
    void verifyAcceptsAnEmptyJournal() {
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<Stored>>any())).thenReturn(List.of());
        assertEquals(OptionalInt.empty(), service.verify());
    }

    @Test
    void verifyAcceptsAnIntactChainReadFromRows() {
        stubRows(false);
        assertEquals(OptionalInt.empty(), service.verify());
    }

    @Test
    void verifyReportsTheRankOfTheFirstAlteredRow() {
        stubRows(true);
        assertEquals(OptionalInt.of(1), service.verify());
    }

    /** Fait renvoyer à query(...) trois lignes construites par le vrai RowMapper de AuditService. */
    private void stubRows(boolean alterSecondRow) {
        when(jdbc.query(anyString(), ArgumentMatchers.<RowMapper<Stored>>any())).thenAnswer(inv -> {
            RowMapper<Stored> mapper = inv.getArgument(1);
            List<Stored> rows = new ArrayList<>();
            String previous = AuditChain.GENESIS;
            for (int i = 0; i < 3; i++) {
                AuditEntry e = entry(i);
                String hash = AuditChain.hash(previous, e);
                ResultSet rs = mock(ResultSet.class);
                when(rs.getTimestamp("occurred_at")).thenReturn(Timestamp.from(e.occurredAt()));
                when(rs.getString("actor")).thenReturn(e.actor());
                when(rs.getString("action")).thenReturn(e.action());
                when(rs.getString("entity_type")).thenReturn(e.entityType());
                when(rs.getString("entity_id")).thenReturn(e.entityId());
                when(rs.getObject("op_id", UUID.class)).thenReturn(e.opId());
                // Ligne 1 : le détail stocké diffère de celui qui a servi au calcul de l'empreinte.
                when(rs.getString("details")).thenReturn(alterSecondRow && i == 1 ? "{\"ecart\":0}" : e.details());
                when(rs.getString("prev_hash")).thenReturn(previous);
                when(rs.getString("hash")).thenReturn(hash);
                rows.add(mapper.mapRow(rs, i));
                previous = hash;
            }
            return rows;
        });
    }
}
