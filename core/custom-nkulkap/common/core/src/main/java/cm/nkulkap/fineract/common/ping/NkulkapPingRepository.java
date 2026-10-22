/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.ping;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Lit la ligne posée par la migration {@code 0001_nkulkap_common_ping.xml} dans la base du locataire courant. */
@Repository
public class NkulkapPingRepository {

    private final JdbcTemplate jdbcTemplate;

    public NkulkapPingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String moduleName() {
        return jdbcTemplate.queryForObject("SELECT module FROM nkulkap_ping WHERE id = 1", String.class);
    }
}
