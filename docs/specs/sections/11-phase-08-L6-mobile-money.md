<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1036 à 1075. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 8 — L6 Mobile money et SMS

**Étape 8.1 — Squelette `nkulkap-pay`**
- **Faire** : Gradle multimodule (`core`, `mtn`, `orange`, `notify`, `interbank`, `app`) ; tables `payment_intent`, `payment_event`, `outbox` ; machine à états (annexe D.1) en code pur testé exhaustivement (toutes les transitions interdites rejetées).
- **Terminé quand** : tests de la machine à états à 100 %.

**Étape 8.2 — Simulateurs**
- **Faire** : `simulators/mtn-momo` et `simulators/orange-money` (WireMock avec scénarios à états) : succès, échec, attente de 10 minutes, rappel en double, rappel absent, 500 intermittents, jeton expiré.
- **Terminé quand** : chaque scénario se déclenche par une valeur particulière du MSISDN de test (documentée).

**Étape 8.3 — Connecteur MTN**
- **Faire** : gestion du jeton, `requestToPay` (encaissement), transfert (décaissement), statut, réception des rappels (FR-L6-02, 04, 05). Resilience4j : délai de 15 s, disjoncteur (FR-L6-11).
- **Terminé quand** : tous les scénarios du simulateur passent.

**Étape 8.4 — Connecteur Orange**
- **Faire** : même contrat que MTN, sur l'API marchande Orange Money obtenue (section 12, entrée I-09).
- **Terminé quand** : tous les scénarios du simulateur passent.

**Étape 8.5 — Comptabilisation**
- **Faire** : comptabilisation dans Fineract (dépôt, remboursement, cotisation) avec `Idempotency-Key = intent_id` ; flux `CASH_OUT` et `DISBURSEMENT` par le compte de transit, avec contre-passation (FR-L6-07) ; frais par grille puis Numscript (FR-L6-10).
- **Terminé quand** : E2E-04 et E2E-05 passent sur simulateurs.

**Étape 8.6 — Interrogation et rapprochement**
- **Faire** : planificateur avec ShedLock (FR-L6-06) ; import des relevés ; moteur d'appariement ; rapport d'écarts (FR-L6-08).
- **Terminé quand** : un relevé fictif de 1 000 lignes avec 12 écarts injectés donne exactement 12 écarts, bien classés.

**Étape 8.7 — Module SMS**
- **Faire** : FR-L6-20 à 23 ; translittération GSM-7 testée (la phrase « Dépôt reçu à Ébolowa » donne « Depot recu a Ebolowa ») ; consommateur de `op.posted`.
- **Terminé quand** : 300 SMS produits pour E2E-01, aucun en double.

**Étape 8.8 — Écrans et app**
- **Faire** : écrans `nkulkap-web` (paiements, rapprochement, écarts) ; dans `nkulkap-field`, déclenchement d'une demande de paiement MoMo par l'agent pour le client, avec suivi du statut.
- **Terminé quand** : parcours validés.

**Étape 8.9 — Sandbox MTN**
- **Faire** : avec les accès du bac à sable (section 12, entrée I-08), exécuter la série de contrats contre le vrai bac à sable. Noter les différences avec le simulateur et corriger le simulateur.
- **Terminé quand** : la série passe ; le simulateur est aligné.

---

