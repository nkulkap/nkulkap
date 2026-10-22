/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.ping;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Sonde du module commun NKULKAP : prouve que le module est chargé et que sa migration est passée.
 *
 * <p>{@code GET /api/v1/nkulkap/ping} renvoie {@code {"status":"ok","module":"nkulkap-common"}}. Authentification
 * Fineract habituelle ; aucune permission métier.
 */
@Component
@Path("/v1/nkulkap/ping")
public class NkulkapPingApiResource {

    private final NkulkapPingRepository repository;

    public NkulkapPingApiResource(NkulkapPingRepository repository) {
        this.repository = repository;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> ping() {
        return Map.of("status", "ok", "module", repository.moduleName());
    }
}
