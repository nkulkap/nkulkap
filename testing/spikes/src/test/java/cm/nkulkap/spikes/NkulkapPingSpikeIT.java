/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 */
package cm.nkulkap.spikes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import cm.nkulkap.spikes.FineractApi.Response;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Étape 1.4 : le module {@code custom/nkulkap/common} est chargé dans l'image {@code fineract-custom}, sa migration
 * {@code db/custom-changelog} est passée et sa ressource JAX-RS répond. Exige la pile (make up).
 */
class NkulkapPingSpikeIT {

    private static final FineractApi API = new FineractApi();

    @Test
    void pingAnswersFromTheCustomModule() throws Exception {
        API.awaitReady(Duration.ofMinutes(1));

        Response r = API.get("/nkulkap/ping");
        System.out.println("(ping) " + r);

        assertEquals(200, r.status());
        assertEquals(Map.of("status", "ok", "module", "nkulkap-common"),
                FineractApi.JSON.convertValue(r.body(), Map.class));
    }

    @Test
    void pingRequiresAuthentication() throws Exception {
        Response r = new FineractApi("inconnu", "mauvais-mot-de-passe").get("/nkulkap/ping");
        System.out.println("(ping sans identifiants valides) " + r);
        assertEquals(401, r.status());
    }
}
