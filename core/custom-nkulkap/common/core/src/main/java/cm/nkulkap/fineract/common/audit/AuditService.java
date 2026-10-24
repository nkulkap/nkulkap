/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.audit;

import cm.nkulkap.fineract.common.audit.AuditChain.Stored;
import java.sql.Timestamp;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Journal d'audit en ajout seul des modules NKULKAP (section 9.1). La table refuse toute modification et toute
 * suppression (déclencheur de la migration {@code 0002_nkulkap_common_audit.xml}) ; le chaînage des empreintes
 * ({@link AuditChain}) rend détectable une altération faite hors de l'application.
 *
 * <p>Les écritures sont sérialisées par un verrou consultatif de transaction, pour que deux lignes ne s'accrochent
 * pas à la même empreinte précédente. La ligne est écrite dans la transaction de l'appelant : une action annulée
 * n'est pas journalisée.
 */
@Service
public class AuditService {

    /** Clé du verrou consultatif PostgreSQL propre au journal d'audit. */
    static final long LOCK_KEY = 0x4E4B_4155_4449_5431L;

    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String record(AuditEntry entry) {
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(?)", Object.class, LOCK_KEY);
        List<String> last = jdbc.queryForList("SELECT hash FROM nkulkap_audit ORDER BY id DESC LIMIT 1", String.class);
        String previous = last.isEmpty() ? AuditChain.GENESIS : last.getFirst();
        String hash = AuditChain.hash(previous, entry);
        jdbc.update(
                "INSERT INTO nkulkap_audit (occurred_at, actor, action, entity_type, entity_id, op_id, details, prev_hash, hash)"
                        + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                Timestamp.from(entry.occurredAt()),
                entry.actor(),
                entry.action(),
                entry.entityType(),
                entry.entityId(),
                entry.opId(),
                entry.details(),
                previous,
                hash);
        return hash;
    }

    /** Vérifie toute la chaîne ; renvoie le rang (à partir de 0) de la première ligne altérée. */
    @Transactional(readOnly = true)
    public OptionalInt verify() {
        List<Stored> rows = jdbc.query(
                "SELECT occurred_at, actor, action, entity_type, entity_id, op_id, details, prev_hash, hash"
                        + " FROM nkulkap_audit ORDER BY id",
                (rs, n) -> new Stored(
                        new AuditEntry(
                                rs.getTimestamp("occurred_at").toInstant(),
                                rs.getString("actor"),
                                rs.getString("action"),
                                rs.getString("entity_type"),
                                rs.getString("entity_id"),
                                rs.getObject("op_id", UUID.class),
                                rs.getString("details")),
                        rs.getString("prev_hash"),
                        rs.getString("hash")));
        return AuditChain.firstBreak(rows);
    }
}
