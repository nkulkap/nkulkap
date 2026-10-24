/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.OptionalInt;

/**
 * Chaînage des lignes d'audit : chaque ligne porte {@code hash = SHA-256(hash précédent, contenu)}. Modifier,
 * supprimer ou insérer une ligne a posteriori casse la chaîne à partir de cette ligne, ce que {@link #firstBreak}
 * détecte. Le champ vide est encodé « \u0000 » pour distinguer null de la chaîne vide.
 */
public final class AuditChain {

    /** Empreinte précédant la première ligne. */
    public static final String GENESIS = "0".repeat(64);

    private static final char SEP = '\u001f';
    private static final String NULL = "\u0000";

    private AuditChain() {}

    public static String hash(String previousHash, AuditEntry e) {
        String content = String.join(
                String.valueOf(SEP),
                previousHash,
                e.occurredAt().toString(),
                e.actor(),
                e.action(),
                e.entityType(),
                orNull(e.entityId()),
                e.opId() == null ? NULL : e.opId().toString(),
                orNull(e.details()));
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponible", ex);
        }
    }

    /** Ligne telle que stockée : contenu, empreinte précédente et empreinte propre. */
    public record Stored(AuditEntry entry, String previousHash, String hash) {}

    /** Index de la première ligne dont l'empreinte ne se vérifie pas ; vide si la chaîne est intègre. */
    public static OptionalInt firstBreak(List<Stored> rows) {
        String expectedPrevious = GENESIS;
        for (int i = 0; i < rows.size(); i++) {
            Stored row = rows.get(i);
            if (!row.previousHash().equals(expectedPrevious)
                    || !row.hash().equals(hash(row.previousHash(), row.entry()))) {
                return OptionalInt.of(i);
            }
            expectedPrevious = row.hash();
        }
        return OptionalInt.empty();
    }

    private static String orNull(String value) {
        return value == null ? NULL : value;
    }
}
