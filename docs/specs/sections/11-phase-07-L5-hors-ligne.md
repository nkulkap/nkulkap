<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 986 à 1035. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 7 — L5 Hors-ligne

**Étape 7.1 — Squelette `nkulkap-sync`**
- **Faire** : espace de travail Cargo (`api`, `inbox`, `poster`, `readmodel`, `common`), axum, sqlx et migrations ; configuration par variables d'environnement ; enregistrement d'appareil (FR-L5-01) avec validation du jeton Keycloak.
- **Terminé quand** : `cargo test` passe ; l'appareil est enregistré et révocable.

**Étape 7.2 — Envoi et boîte de réception**
- **Faire** : `POST /v1/sync/push` (annexe C) : vérification de la signature Ed25519 du lot, décompression gzip, validation du schéma, insertion transactionnelle dans la boîte de réception (FR-L5-02, FR-L5-03), détection d'écart de `seq` et contrôle de dérive d'horloge (FR-L5-08).
- **Tests** : proptest « toute séquence de lots, avec doublons et permutations, donne exactement l'ensemble des `op_id` uniques ».
- **Terminé quand** : tests passés, y compris un lot corrompu, une mauvaise signature et un appareil révoqué.

**Étape 7.3 — Comptabilisation**
- **Faire** : travailleur de comptabilisation (FR-L5-04) : files par compte, verrouillage consultatif PostgreSQL par compte, appel à Fineract avec `Idempotency-Key`, catégorisation des erreurs (réseau / métier / inconnue) et attente exponentielle (1 s, 2 s, 4 s… plafond 5 min, sans limite de durée pour les erreurs réseau). Appel au module collecte pour `nkulkap_collecte_op` ; publication `op.posted` et `op.rejected`.
- **Terminé quand** : E2E-02 au niveau du service (Toxiproxy entre `nkulkap-sync` et Fineract) : 0 doublon sur 1 000 opérations.

**Étape 7.4 — Modèle de lecture et réception**
- **Faire** : consommateur Kafka (Avro) ; instantané initial par l'API ; `GET /v1/sync/pull` paginé avec curseur (FR-L5-05, FR-L5-06).
- **Terminé quand** : un dépôt fait au guichet apparaît dans la réception de l'appareil du collecteur concerné en moins de 10 secondes (réseau normal), et seulement chez lui.

**Étape 7.5 — File de revue du superviseur**
- **Faire** : API et écran `nkulkap-web` des opérations `REJECTED_NEEDS_REVIEW`. Actions : approuver par retrait forcé, réaffecter au compte d'attente, annuler avec motif. Maker-checker obligatoire.
- **Terminé quand** : chaque action est testée et auditée.

**Étape 7.6 — `nkulkap-field` : boîte d'envoi**
- **Faire** : ADR sur le chiffrement de la base locale (SQLCipher ou chiffrement applicatif, selon la compatibilité avec Room Kotlin Multiplatform). Nouvelle entité `OutboxOp` (`op_id`, `seq`, `hlc`, `type`, `payload`, `statut`, `tentatives`, `derniere_erreur`). Réécriture des flux hors-ligne listés à l'étape 1.6 pour passer par la boîte d'envoi (FR-L5-10, FR-L5-11). Travail WorkManager (FR-L5-12), indicateurs (FR-L5-14), plafonds (FR-L5-15), budget de données (FR-L5-13).
- **Règle MPL** : écrire le nouveau code dans de **nouveaux fichiers et modules** (`feature/nkulkap-*`, `core/nkulkap-*`) et limiter les modifications de fichiers existants.
- **Terminé quand** : tests d'instrumentation « arrêt forcé entre la saisie et l'envoi » et « redémarrage pendant l'envoi », sans perte ni doublon.

**Étape 7.7 — `nkulkap-field` : écrans métier**
- **Faire** : ouverture et clôture de tournée, liste des carnets du jour (ordre de passage), saisie de collecte, reçu avec code court (FR-L4-07), cotisations de tontine, GPS, impression Bluetooth optionnelle. Écrans FR et EN.
- **Terminé quand** : parcours validés sur l'appareil de référence bas de gamme (NFR-08).

**Étape 7.8 — `nkulkap-edge` : réplique et protocole**
- **Faire** : binaire Rust ; base SQLite (WAL, `synchronous=FULL`) ; client du protocole sync (même code que les tests de `nkulkap-sync`) ; réplication du portefeuille de l'agence (FR-L5-20, FR-L5-21).
- **Terminé quand** : réplication initiale de 20 000 clients, puis synchronisation différentielle.

**Étape 7.9 — `nkulkap-edge` : guichet dégradé**
- **Faire** : interface askama + htmx (FR-L5-22 à 24) ; bascule automatique ou manuelle (FR-L5-23) ; impression ESC/POS ; authentification locale des caissiers (empreintes de mots de passe répliquées, sessions locales).
- **Terminé quand** : E2E-03 passe.

**Étape 7.10 — `nkulkap-edge` : exploitation**
- **Faire** : service systemd, mises à jour signées avec retour arrière (FR-L5-26), journalisation tournante, compilation croisée x86_64 et aarch64 (`musl`), script d'installation.
- **Terminé quand** : E2E-11 passe et une mise à jour volontairement défectueuse est annulée automatiquement.

**Étape 7.11 — Campagne de résilience**
- **Faire** : exécuter E2E-01, E2E-02, E2E-03, E2E-11 et E2E-12 dans la CI nocturne, avec rapport des volumes de données (NFR-04).
- **Terminé quand** : 5 nuits consécutives vertes.

---

