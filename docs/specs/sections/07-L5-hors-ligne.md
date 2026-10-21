<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 477 à 513. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L5 — Hors-ligne (`nkulkap-field`, `nkulkap-sync`, `nkulkap-edge`)

#### L5a — `nkulkap-sync` (passerelle Rust)

- **FR-L5-01** — Enregistrement d'appareil : l'utilisateur authentifié (Keycloak) enregistre la clé publique Ed25519 générée sur l'appareil. L'appareil est lié à un utilisateur et à une agence, et révocable.
- **FR-L5-02** — Envoi (`POST /v1/sync/push`) : un lot signé contient des opérations portant `op_id`, `seq` (compteur monotone par appareil), `hlc`, `type`, `compte`, `montant`, `horodatage_appareil`, `gps` et des données propres au type. Réponse **par opération** : `ACCEPTED`, `DUPLICATE` ou `REJECTED{code}`. `ACCEPTED` signifie « stocké durablement », pas encore « comptabilisé ».
- **FR-L5-03** — Boîte de réception : `op_id` est la clé primaire. L'insertion et l'accusé de réception se font dans la même transaction. Un doublon renvoie le statut existant.
- **FR-L5-04** — Comptabilisation : un travailleur traite les opérations **dans l'ordre, par compte** (partitionnement par compte) et en parallèle entre comptes. Il appelle Fineract avec `Idempotency-Key = op_id`. Erreurs réseau : nouvelles tentatives avec attente exponentielle plafonnée et la même clé. Erreur métier (compte clôturé, solde insuffisant) : `REJECTED_NEEDS_REVIEW` et envoi dans la file du superviseur.
- **FR-L5-05** — Réception (`GET /v1/sync/pull?cursor=`) : renvoie les statuts d'opérations, les changements du portefeuille de l'utilisateur (clients, comptes, soldes, échéances dues, carnets) et la configuration (plafonds, produits, types de paiement). Pages limitées à 200 Ko compressés, avec un curseur opaque.
- **FR-L5-06** — Modèle de lecture : `nkulkap-sync` consomme les événements Kafka de Fineract pour maintenir un instantané par portefeuille, ce qui permet de calculer les différences sans solliciter Fineract. L'instantané initial se construit par l'API Fineract.
- **FR-L5-07** — Politique de conflits (table complète en annexe C.4) : un dépôt ou un remboursement est toujours tenté, car l'argent a été physiquement reçu. Un retrait hors-ligne refusé pour solde insuffisant passe en revue superviseur, qui peut l'approuver par `command=force-withdrawal` (API Fineract existante).
- **FR-L5-08** — Temps : la date de valeur est la date serveur de réception, sauf pour les opérations de la veille reçues avant l'heure limite configurée (par défaut 10 h) **[À VÉRIFIER : règle comptable de l'EMF]**. Un appareil dont l'horloge dérive de plus de 15 minutes reçoit un avertissement ; au-delà de 24 heures, ses lots sont refusés jusqu'à resynchronisation de l'horloge.
- **FR-L5-09** — Événements sortants : `op.posted` et `op.rejected` sont publiés sur Kafka. Ils déclenchent les SMS (`nkulkap-pay/notify`) et la surveillance (`nkulkap-sentinel`).

#### L5b — `nkulkap-field` (application Android)

- **FR-L5-10** — Toute opération transactionnelle (collecte, remboursement, dépôt, retrait, cotisation de tontine, création de client) passe par une **file sortante locale** (boîte d'envoi). Les appels directs à l'API Fineract pour ces opérations sont supprimés.
- **FR-L5-11** — `op_id` (UUIDv7) est créé et persisté dans la même transaction SQLite que l'opération, avant tout affichage de reçu.
- **FR-L5-12** — Synchronisation automatique en arrière-plan (WorkManager), déclenchée par le retour du réseau, avec attente exponentielle. Un bouton « Synchroniser maintenant » reste disponible.
- **FR-L5-13** — Budget de données : paramètre « Wi-Fi uniquement pour les téléchargements lourds » et compteur d'octets consommés par jour.
- **FR-L5-14** — Indicateurs visibles en permanence : nombre d'opérations en attente, âge de la plus ancienne, heure de la dernière synchronisation réussie.
- **FR-L5-15** — Contrôle des plafonds hors-ligne avant chaque opération (FR-L4-01).
- **FR-L5-16** — Sécurité locale : base chiffrée, verrouillage par code, effacement à distance à la révocation de l'appareil, déconnexion après inactivité.
- **FR-L5-17** — Reçu : écran de reçu avec code court ; impression Bluetooth ESC/POS en option.

#### L5c — `nkulkap-edge` (nœud d'agence)

- **FR-L5-20** — Le nœud d'agence est un « gros appareil » qui parle le même protocole que le téléphone (L5a), avec sa propre clé d'appareil.
- **FR-L5-21** — Il réplique le portefeuille de l'agence (clients, comptes, soldes, échéanciers) dans SQLite en mode WAL avec `synchronous=FULL`.
- **FR-L5-22** — Il sert une interface web de guichet en mode dégradé (HTML rendu côté serveur + htmx) sur le réseau local : dépôt, retrait dans la limite des plafonds, remboursement, consultation de solde (avec date de fraîcheur), impression de reçu.
- **FR-L5-23** — Quand le lien central fonctionne, le guichet normal (`nkulkap-web`) est utilisé. Le mode dégradé s'active automatiquement après 60 secondes de lien coupé, ou manuellement par le chef d'agence.
- **FR-L5-24** — Solde disponible hors-ligne = dernier solde synchronisé − retraits en attente − solde minimum. Il est plafonné par opération et par client et par jour.
- **FR-L5-25** — Autonomie : 72 heures sans lien central sans perte ni blocage.
- **FR-L5-26** — Mises à jour : téléchargées quand le lien est disponible, vérifiées par signature Ed25519, installées par remplacement atomique, avec retour arrière automatique si le contrôle de santé échoue.

**Acceptation (L5)** : scénarios E2E-01, E2E-02, E2E-03 et E2E-11.

