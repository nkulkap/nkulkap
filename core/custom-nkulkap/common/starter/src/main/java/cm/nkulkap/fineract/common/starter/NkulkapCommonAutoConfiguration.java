/*
 * Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
 * Logiciel propriétaire NKULKAP : voir le fichier LICENSE à la racine du dépôt.
 */
package cm.nkulkap.fineract.common.starter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/** Charge les composants du module commun NKULKAP dans Fineract (déclarée dans AutoConfiguration.imports). */
@AutoConfiguration
@ComponentScan("cm.nkulkap.fineract.common")
public class NkulkapCommonAutoConfiguration {}
