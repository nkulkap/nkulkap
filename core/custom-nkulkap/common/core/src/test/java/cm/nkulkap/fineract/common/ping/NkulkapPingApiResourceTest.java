/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.ping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class NkulkapPingApiResourceTest {

    @Test
    void pingReturnsModuleNameReadFromTheMigratedTable() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT module FROM nkulkap_ping WHERE id = 1", String.class))
                .thenReturn("nkulkap-common");

        Map<String, String> body = new NkulkapPingApiResource(new NkulkapPingRepository(jdbc)).ping();

        assertEquals(Map.of("status", "ok", "module", "nkulkap-common"), body);
    }
}
