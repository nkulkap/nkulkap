/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class MessagesBundleTest {

    private static Properties load(String name) throws IOException {
        try (InputStream in = MessagesBundleTest.class.getResourceAsStream(name)) {
            assertNotNull(in, "ressource introuvable : " + name);
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                Properties p = new Properties();
                p.load(reader);
                return p;
            }
        }
    }

    @Test
    void sameKeysInAllBundles() throws IOException {
        Properties fr = load("/i18n/messages_fr.properties");
        Properties en = load("/i18n/messages_en.properties");
        Properties def = load("/i18n/messages.properties");
        assertFalse(fr.isEmpty());
        assertEquals(fr.keySet(), en.keySet());
        assertEquals(fr.keySet(), def.keySet());
    }

    @Test
    void noEmptyValue() throws IOException {
        for (String name : new String[] {"fr", "en"}) {
            Properties p = load("/i18n/messages_" + name + ".properties");
            for (String key : p.stringPropertyNames()) {
                assertFalse(p.getProperty(key).isBlank(), name + " : valeur vide pour " + key);
            }
        }
    }

    @Test
    void everyMessageFormatsWithTwoArguments() throws IOException {
        for (String name : new String[] {"fr", "en"}) {
            Properties p = load("/i18n/messages_" + name + ".properties");
            for (String key : p.stringPropertyNames()) {
                String formatted = new MessageFormat(p.getProperty(key)).format(new Object[] {"A1", "B2"});
                assertFalse(formatted.isBlank(), name + " : " + key);
            }
        }
    }

    @Test
    void keysAreAscii() throws IOException {
        for (String name : new String[] {"fr", "en"}) {
            for (String key : load("/i18n/messages_" + name + ".properties").stringPropertyNames()) {
                assertTrue(key.chars().allMatch(c -> c < 128), name + " : clé non ASCII " + key);
            }
        }
    }
}
