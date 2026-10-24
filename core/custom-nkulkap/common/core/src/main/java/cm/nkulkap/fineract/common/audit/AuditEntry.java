/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.audit;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Ligne du journal d'audit NKULKAP ({@code nkulkap_audit}). {@code details} est un JSON libre ; {@code opId} est
 * l'opération à l'origine de l'action, s'il y en a une.
 */
public record AuditEntry(
        Instant occurredAt,
        String actor,
        String action,
        String entityType,
        String entityId,
        UUID opId,
        String details) {

    public AuditEntry {
        Objects.requireNonNull(occurredAt, "occurredAt");
        requireText(actor, "actor");
        requireText(action, "action");
        requireText(entityType, "entityType");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " obligatoire");
        }
    }
}
