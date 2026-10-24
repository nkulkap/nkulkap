# NKULKAP — Avancement

Dernière mise à jour : 2026-10-22. Plan détaillé : une fiche par phase dans [specs/sections/](specs/README.md) (référence complète : [cahier des charges](specs/NKULKAP_cahier_des_charges_et_plan.md)).

## Phases

Modèle conseillé : **Opus** conçoit, écrit la logique critique et relit ; **Sonnet** fait le travail mécanique via les sous-agents de `.claude/agents/` ; **Haiku** explore le code (`fineract-explorer`). Ce qui ne se délègue jamais est listé dans `CLAUDE.md`.

| Phase | Contenu | Statut | Modèle conseillé |
|---|---|---|---|
| 0 | Préparer : dépôts, outillage, pile de développement | Terminée et fusionnée le 2026-10-22 | Sonnet pour squelette, ADR, Compose, CI ; Opus pour Makefile, choix du fork, relecture |
| 1 | Comprendre : explorations et vérifications d'hypothèses | En cours | Opus pilote et conclut ; Haiku (`fineract-explorer`) pour les recherches |
| 2 | Socle commun : contrats, données de test, réseau dégradé, identité | En cours | Opus pour contrats et identité (sécurité) ; Sonnet pour fixtures FICTIVES, profils Toxiproxy, Keycloak en suivant le modèle |
| 3 | L1 Plan comptable et hors-bilan | À faire | Opus pour les règles comptables et le hors-bilan ; Sonnet pour CRUD, ressources REST, migrations |
| 4 | L2 TEG et usure | À faire | Opus pour le calcul du TEG et son premier test ; Sonnet (`nkulkap-tests`) pour les cas supplémentaires, `nkulkap-i18n` pour la fiche TEG |
| 5 | L3 Numscript | À faire | Opus (répartitions Numscript) ; Sonnet pour l'adaptateur Go hors logique et les tests supplémentaires |
| 6 | L4 Collecte et tontine | À faire | Opus pour la conception et les calculs ; Sonnet pour CRUD, jobs, i18n |
| 7 | L5 Hors-ligne (sync, app, nœud d'agence) | À faire | Opus (idempotence et synchronisation, jamais délégués) ; Sonnet pour écrans Android, i18n, empaquetage |
| 8 | L6 Mobile money et SMS | À faire | Opus pour les machines à états de `nkulkap-pay` ; Sonnet pour simulateurs WireMock, SMS GSM-7, tests |
| 9 | L7 Reporting réglementaire | À faire | Opus pour la correspondance comptes → états ; Sonnet pour gabarits YAML et exports |
| 10 | L8 LAB-FT | À faire | Opus (règles LAB-FT, jamais déléguées) ; Sonnet pour les tests supplémentaires |
| 11 | L9 Interbancaire (adaptateur par fichiers) | À faire | Opus pour le rapprochement ; Sonnet pour lecteurs et écrivains de fichiers |
| 12 | Bout en bout et industrialisation | À faire | Sonnet pour Helm, CI, scénarios E2E ; Opus relit |
| 13 | Migration des données et pilote | À faire | Opus (migration de données existantes, jamais déléguée) |

## Phase 0 — détail

| Étape | Statut | Vérification |
|---|---|---|
| 0.1 Monodépôt | Fait | Arborescence de la section 6 présente ; `CLAUDE.md` et `docs/STATUS.md` présents ; premier commit fait. `make help` non exécutable (GNU Make absent du poste) : la commande de la cible `help` a été lancée à la main et liste les 10 cibles. |
| 0.2 Fork Fineract et modules custom | Fait (2026-10-22) | Fork public `nkulkap/nkulkap-fineract` (branche par défaut `nkulkap/main` = `b94b2ddf`), sous-module `core/fineract`, module `core/custom-nkulkap/common/core` (calqué sur `custom/acme/note/service`), jonction `core/fineract/custom/nkulkap` (ADR-013). `./gradlew projects` liste `:custom:nkulkap:common:core` ; `settings.gradle` identique à `b94b2ddf` ; `./gradlew :custom:nkulkap:common:core:build` (spotless compris, ADR-015) : BUILD SUCCESSFUL. |
| 0.3 ADR | Fait | 13 fichiers dans `docs/adr/` (001 à 012 du cahier, 013 pour la jonction Windows), tous liés depuis `docs/adr/README.md`. |
| 0.4 Pile de développement | Fait (2026-10-22) | `deploy/compose/dev.yml` : PostgreSQL 18.3, Kafka 4.3.2 (KRaft), Keycloak 26.8.1, Fineract (`fineract-custom`, construite par `./gradlew :custom:docker:jibDockerBuild`), Toxiproxy 2.13.0 ; Prometheus, Grafana et Loki sous le profil `obs`. Contrôles de santé sur chaque service ; fuseau du locataire `Africa/Douala` vérifié en base. Démarrage (images présentes, base vide) jusqu'à `GET /offices` = 200 : 238 s sur un poste de 7,4 Go ; ≈ 2,1 Go consommés. `make down` : 0 conteneur, volume ou réseau restant. Réserve : profil `obs` non démarré (mémoire du poste) ; `wget` dans l'image Loki non vérifié. |
| 0.5 Intégration continue | Fait (2026-10-22) | GitHub Actions (ADR-014) : `ci-core`, `ci-sync`, `ci-edge`, `ci-pay`, `ci-rules`, `ci-sentinel`, `ci-field`, `ci-web` déclenchés par chemin, et `e2e-nightly` (vide). Cache Gradle, Cargo, sbt, Go et npm. Vérifié : le commit `7bd2c9a`, qui ne touche que `core/custom-nkulkap`, ne déclenche que `ci-core`, qui passe ([exécution 39782827373](https://github.com/nkulkap/nkulkap/actions/runs/39782827373)). Spotless actif sans dépendre d'Eclipse (ADR-015). `e2e-nightly` actif sur `main` depuis la fusion. |

### Poste de développement (Windows, 7,4 Go de RAM)

- JDK 25 (`Microsoft.OpenJDK.25`) installé ; GNU Make 4.4.1 installé par winget (`ezwinports.make`, sous `%LOCALAPPDATA%\Microsoft\WinGet\Packages\ezwinports.make_*\bin`), à lancer depuis Git Bash. `make help` et `make bootstrap` vérifiés sur le poste le 2026-10-22.
- Docker Desktop (WSL2) limité à 5 Go par `%USERPROFILE%\.wslconfig` (`memory=5GB`, `swap=8GB`).
- `~/.gradle/gradle.properties` ramène Gradle à `-Xmx3g`, 2 workers, sans parallélisme : le `-Xmx12g` de Fineract fait planter la JVM sur ce poste.
- Le sous-module Fineract doit être extrait en LF (`core.autocrlf=false`) : en CRLF, le build réécrit les sources et échoue. `make bootstrap` le règle.
- Ne pas compiler Fineract pendant que la pile Docker tourne (mémoire insuffisante pour les deux).
- Node 18.14 seulement sur le poste : `make lint-contracts` exige Node ≥ 22.12 ; en local, lancé dans le conteneur `node:22.12.0-alpine`.
- Go 1.27.0 installé par winget (`GoLang.Go`, `C:\Program Files\Go\bin`) le 2026-10-22 ; `go.mod` de `nkulkap-rules` exige Go ≥ 1.24.4 (minimum de Numscript).
- Windows Defender : dossiers du projet et `~/.gradle` exclus de l'analyse en temps réel. Sans cela, la compilation de Fineract échoue par « Ressources système insuffisantes » (erreur 1450). La construction de l'image prend environ 11 min avec `-Dorg.gradle.workers.max=1` et `-Xmx2560m`.

### Dépôts

- Monodépôt : https://github.com/nkulkap/nkulkap (public).
- Fork : https://github.com/nkulkap/nkulkap-fineract (public, fork d'`apache/fineract`). Base `b94b2ddf` plutôt que l'étiquette `1.16.0`, car celle-ci (19/10/2026) est antérieure au commit de référence (20/10/2026), conformément à la règle de l'étape 0.2.
- Sous Git pour Windows, cloner Fineract avec `-c http.version=HTTP/1.1 -c core.longpaths=true` : en HTTP/2, le clone échoue (`curl 92`, `early EOF`).

### Bilan de la Phase 0

- Fusionnée dans `main` le 2026-10-22 : [PR #1](https://github.com/nkulkap/nkulkap/pull/1), commit de fusion `75bab1b`.
- `e2e-nightly` actif sur `main` (planifié chaque nuit à 01:00 UTC) ; déclenchement manuel réussi ([exécution 39786846059](https://github.com/nkulkap/nkulkap/actions/runs/39786846059)).
- `CLAUDE.md` fait lire la fiche de la phase en cours plutôt que le cahier complet (accord de l'utilisateur du 2026-10-22).

## Phase 1 — détail

| Étape | Statut | Vérification |
|---|---|---|
| 1.1 Tour de Fineract | Fait (2026-10-22) | `make seed` (`testing/fixtures/seed/seed.py`, Python sans dépendance) crée par API, avec des données FICTIVES : devise XAF, 2 agences, 3 employés, 14 comptes comptables, 1 produit d'épargne et 1 produit de prêt en comptabilité de caisse, 10 clients, 10 comptes d'épargne avec dépôt, 1 prêt décaissé. Rejouable : vérifié sur base vierge (`make down && make up`, puis `make seed` deux fois) ; `make seed-test` (seed deux fois, état identique) : OK. Note [fineract-api-usage.md](specs/fineract-api-usage.md) : `glaccounts`, `journalentries`, `savingsaccounts/{id}/transactions` (`deposit`, `withdrawal`, `force-withdrawal`), `loans/{id}/transactions` (`repayment`), `runreports`, `externalevents/configuration`, avec réponses et erreurs observées. |
| 1.2 Idempotence de Fineract | Fait (2026-10-22) | `IdempotencySpikeIT` (`testing/spikes`, `make spike-test SPIKE=IdempotencySpikeIT`, pile et seed requis) : 9 cas verts. **Une seule transaction dans les cas (a) à (e)** : rejeu servi depuis le cache (`x-served-from-cache: true`, corps identique), y compris en concurrence (4 envois × 10 tours) et après redémarrage de Fineract. Note [idempotence-fineract.md](specs/idempotence-fineract.md). Aucun doublon, donc pas de table de déduplication ; risques relevés (clé unique par action/entité et non par compte, refus mis en cache, clé bloquée « en cours » après arrêt brutal) et parade par `externalId` = `op_id` : [ADR-016](adr/0016-double-garde-idempotency-key-et-externalid.md), accepté le 2026-10-22. |
| 1.8 Retrait forcé | Fait (2026-10-22) | `ForceWithdrawalSpikeIT` (`make spike-test SPIKE=ForceWithdrawalSpikeIT`) : 3 cas verts. Note [force-withdrawal.md](specs/force-withdrawal.md) : interrupteur **global au locataire** `allow-force-withdrawal-on-savings-account` (plafond dans `value`, désactivé par défaut, aucun réglage par produit) ; permission propre `FORCE_WITHDRAWAL_SAVINGSACCOUNT` ; transaction de type `Withdrawal` ; part à découvert débitée sur `overdraftPortfolioControlId`. Point à trancher en phase 6 : restreindre le retrait forcé à certains produits. |
| 1.3 Événements externes Kafka | Fait (2026-10-22) | `ExternalEventsSpikeIT` (`make spike-test SPIKE=ExternalEventsSpikeIT`) : lit et décode (classes Avro de `fineract-avro-schemas`) un `SavingsDepositBusinessEvent` dont l'`id` et le montant correspondent au dépôt posté ; vert. `make seed` active 8 types (clients, dépôt, retrait, retrait forcé, décaissement, remboursement, solde du prêt). Note [fineract-events.md](specs/fineract-events.md) : enveloppe `MessageV1`, champs utiles, latence 44 à 60 s (job d'envoi chaque minute). **Hypothèse du cahier fausse** : aucun événement d'écriture comptable pour les prêts et l'épargne ; cahier corrigé (étape 1.3) et [ADR-017](adr/0017-ecritures-comptables-sans-evenement-externe.md), accepté le 2026-10-22. |
| 1.4 Module custom minimal | Fait (2026-10-22) | `common/core` : migration `db/custom-changelog/0001_nkulkap_common_ping.xml` (table `nkulkap_ping`), ressource `GET /api/v1/nkulkap/ping` ; `common/starter` : auto-configuration (`AutoConfiguration.imports`). Image `fineract-custom` reconstruite : `curl …/api/v1/nkulkap/ping` → `{"module":"nkulkap-common","status":"ok"}` (valeur lue dans la table migrée). `make test COMPONENT=core` et `make build` (Spotless compris) : OK ; `NkulkapPingSpikeIT` : 2 cas verts. Note [module-custom-fineract.md](specs/module-custom-fineract.md). |
| 1.5 Veto avant décaissement | Fait (2026-10-22) | `DisbursalVetoSpikeListener` (`common/core`), enregistré par `addPreBusinessEventListener(LoanDisbursalBusinessEvent.class, …)`, actif seulement si `NKULKAP_SPIKE_DISBURSAL_VETO_ENABLED=true` (pile de développement). `DisbursalVetoSpikeIT` : 2 cas verts. Prêt `TEST-VETO-…` : 403, code `error.msg.nkulkap.loan.disbursal.vetoed` remonté dans `errors[0]`, prêt resté `Approved`, aucune transaction ni écriture (annulation complète) ; prêt témoin décaissé. Tests unitaires du module : 4/4. Point d'extension validé, pas d'ADR. |
| 1.6 Audit de l'app Android | Partielle (2026-10-22) | Fork `nkulkap/nkulkap-field` (d'`openMF/mifos-x-field-officer-app`, nouveau nom d'`android-client`, MPL-2.0) en sous-module `apps/nkulkap-field` (branche `dev`, `b5a57c95`). Audit par lecture du code : note [field-offline-audit.md](specs/field-offline-audit.md). **Absence d'idempotence confirmée** (aucun `Idempotency-Key` ni `externalId` d'opération) ; défauts graves : remboursement non supprimé après succès (mauvaise table, renvoi en double), suppression de toutes les transactions d'épargne du compte après une seule, clé de remboursement en secondes écrasée par `REPLACE`. Fichiers à modifier en phase 7 listés. **Reste** : compilation et parcours sur émulateur, reportés au poste puissant (RAM). |
| 1.7 Numscript | Fait (2026-10-22) | `services/nkulkap-rules` (Go, Numscript v0.0.24) : `internal/engine` exécute une règle en calcul pur (magasin de soldes vide, montants entiers `int64`) ; commande `cmd/nkulkap-rules <règle.num> <entrée.json>`. Règles de l'annexe B dans `rules/collecte/frais_collecte.num` et `rules/momo/frais_retrait.num`. `make test COMPONENT=rules` (`go vet` + `go test ./...`) : vert ; sorties exactes de l'annexe B retrouvées (1 240 / 1 860 ; 162 / 252 / 586) et somme conservée. |

Constats de 1.1 utiles à la suite : XAF doit être déclarée dans `/currencies` sinon les écritures manuelles sont refusées ; `GET /journalentries` ne filtre pas par `referenceNumber` ; `PUT /externalevents/configuration` avec un type inconnu renvoie 500.

## Phase 2 — détail

| Étape | Statut | Vérification |
|---|---|---|
| 2.1 Contrats | Fait (2026-10-22) | `contracts/sync-v1.openapi.yaml` (annexe C, D.2, FR-L5), `pay-v1.openapi.yaml` (D.1, FR-L6), `sentinel-v1.openapi.yaml` (FR-L8), en OpenAPI 3.1 ; événements Avro dans `contracts/events/` : enveloppe `EventEnvelopeV1` (calquée sur `MessageV1` de Fineract), `op.posted`, `op.rejected`, `payment.*`, `tournee.*`, chacun avec un exemple FICTIF. `make lint-contracts` (Redocly CLI 2.60.0, règles `recommended` renforcées ; `avsc` 5.7.9 : syntaxe, conventions, montants entiers, aller-retour des exemples) : vert ; CI `ci-contracts`. Génération de code reportée à la phase de chaque service (`contracts/README.md`). |
| 2.2 Générateur de données fictives | Fait (2026-10-22) | `testing/fixtures/generator/` : `make fixtures SIZE=small\|large` génère dans `testing/fixtures/out/<SIZE>/` (non versionné) des fichiers JSON Lines et un manifeste à empreintes SHA-256 ; graine fixe 20261022, date de fin fixe 2026-09-30, aucune dépendance à l'heure. Small : 2 agences, 5 collecteurs, 200 clients, 60 prêts, 1 013 mouvements, empreinte globale `cc36adb2…` identique à chaque exécution. Large : 10 agences, 50 collecteurs, 20 000 clients, 6 000 prêts, 311 004 mouvements, 12 mois (≈ 4 s, 47 Mo). Noms camerounais francophones et anglophones (20 %), numéros FICTIFS `+2376999…`, identifiants `FICTIF-S-…` / `FICTIF-L-…`. `make fixtures-test` : 10 tests verts. `make fixtures-load SIZE=small` : chargé par l'API sur base vierge en 263 s ; rejeu complet sans fichier de reprise : 2 185 écritures avant et après, aucun doublon. **Reste** : chargement du jeu large non fait sur ce poste (311 004 mouvements, plusieurs heures). |
| 2.3 Harnais de réseau dégradé | Fait (2026-10-22) | `testing/toxiproxy/` : `profiles.yaml` (5 profils de la section 10.2 ; pertes simulées par `reset_peer` sur une fraction des connexions), commande Go `toxictl` (`init`, `apply`, `reset`, `list`) appelable depuis Rust, Java, Scala et les scripts E2E (`README.md`), `make toxiproxy PROFILE=...`. Mesuré : 0,17 s sans toxique, 2,7 s sous `vsat_agence`. `make toxiproxy-test` : vert ; `reponse_perdue` démontré : dépôt envoyé par le mandataire, aucun octet de réponse (EOF), dépôt présent côté serveur (relecture par `externalId`), rejeu servi depuis le cache sans doublon. Toxique ajouté après la poignée de main TLS, sinon la requête ne part jamais. |
| 2.4 Identité | Fait (2026-10-22) | Royaume Keycloak `nkulkap` (`deploy/keycloak/provision.py`, export versionné `realm-nkulkap.json` importé au démarrage, `make identity-provision`, `make identity-export`) : 7 rôles de la section 9.1, clients `nkulkap-web` et `nkulkap-field` (code + PKCE), `nkulkap-sync`, `nkulkap-pay`, `nkulkap-sentinel` (comptes de service), `nkulkap-tests` (développement). TOTP exigé pour comptable, conformité et administrateur (rôle composite `mfa_requise`, flux navigateur et mot de passe direct). Fineract en serveur de ressources : surcouche `make up AUTH=oidc` (fédération OIDC de Fineract, utilisateurs et rôles Fineract de même nom). `make identity-test` : 8 tests verts, rejouables : un collecteur obtient un jeton et appelle `GET /nkulkap/ping` (200), il est refusé sur les écritures comptables (403 `error.msg.not.authorized`) ; comptable sans code ou avec un faux code refusé ; jeton falsifié et requête anonyme refusés (401). [ADR-018](adr/0018-identite-keycloak-roles-fineract.md), **statut Proposé, à valider**. Défaut Fineract amont contourné : dépendance circulaire au démarrage en mode OIDC (`SPRING_MAIN_ALLOWCIRCULARREFERENCES=true`). |
| 2.5 Observabilité | À faire | Non commencée. Une trace de bout en bout d'une requête de synchronisation exige `nkulkap-sync` (phase 7) et le profil `obs` (mémoire) : à faire sur le poste puissant. |
| 2.6 Bibliothèque commune NKULKAP (Java) | Fait (2026-10-24) | `common/core` : `money/Money` (montant `long`, opérations exactes), `id/UuidV7` (RFC 9562, monotone), `journal/JournalEntryBuilder` (FR-L3-04 : une écriture équilibrée, `referenceNumber` = `op_id`), journal d'audit `audit/AuditService` + `AuditChain` (chaîne SHA-256) avec migration `0002_nkulkap_common_audit.xml` (déclencheurs refusant `UPDATE`, `DELETE`, `TRUNCATE`), catalogue `error/NkulkapErrorCode` + `NkulkapException`, client `rules/RulesClient` (`POST /v1/evaluate`, délais 2 s / 5 s, 3 nouvelles tentatives sur 502/503/504 et erreurs réseau), messages `i18n/messages_fr|en.properties`. `make build` (Spotless, Checkstyle, SpotBugs, JaCoCo ≥ 90 % branché sur `check`) : vert, 50 tests, **couverture de lignes 99,0 %** (296/299). Image reconstruite : migrations `0001` et `0002` passées, déclencheurs présents, ping OK, `DisbursalVetoSpikeIT` vert. Répartition : modèles et premiers tests par Opus ; catalogue, client, JaCoCo par `nkulkap-boilerplate` ; messages par `nkulkap-i18n` ; tests complémentaires par `nkulkap-tests`. |

## À faire sur le poste puissant

Bascule prévue sur le PC fixe de l'utilisateur (2026-10-24) : reprendre à l'étape 2.5 en lisant ce fichier (2.6 terminée). L'accès des sous-agents, coupé le 2026-10-24 (HTTP 403 `oauth_org_not_allowed`), a été rétabli le jour même par l'utilisateur.

Le poste de développement (7,4 Go de RAM, barrette d'extension incompatible) ne fait pas tourner certains travaux. Ils attendent le PC fixe de l'utilisateur, aujourd'hui hors réseau :
- 1.6 : compilation de `apps/nkulkap-field` et parcours du mode hors-ligne sur émulateur Android (l'audit par lecture du code se fait sur ce poste).
- Phase 7 : écrans Android et tests sur émulateur.
- Phase 12 : scénarios de bout en bout avec la pile complète et le profil `obs` ; tests de charge.

## Écarts

- Projet renommé NKULAP → NKULKAP. Le cahier a été copié dans `docs/specs/` avec le renommage appliqué ; l'original reste dans Téléchargements.
- Dans la copie, la phrase sur la recherche web d'antériorité précise désormais qu'elle portait sur « Nkulap ». Elle est marquée **[À VÉRIFIER]** : refaire la recherche pour « Nkulkap ».
- Le cahier est découpé en 44 fiches dans `docs/specs/sections/`, générées par `scripts/split-spec.sh`. La copie complète reste la référence.
- `apps/nkulkap-field` est un sous-module (fork openMF) ; `apps/nkulkap-web` reste à créer.
- Liaison des modules custom sous Windows par jonction NTFS plutôt que par lien symbolique ou copie (ADR-013).
- Groupe Gradle `cm.nkulkap.fineract` choisi par analogie avec `com.acme.fineract` : le cahier ne fixe pas de nom de paquet.
- Modules NKULKAP : conventions Gradle propres (`core/custom-nkulkap/nkulkap-conventions.gradle`, ADR-015) — palantir-java-format, en-tête propriétaire Circaura Sarl, sans audit Apache RAT ni en-tête Apache.
- Pile locale en deux modes d'authentification : `AUTH=basic` (défaut, outillage) et `AUTH=oidc` (Keycloak) ; en mode OIDC, Fineract refuse l'authentification Basic (ADR-018).
- Kafka de la pile locale : écouteur supplémentaire `HOST` annoncé sur `localhost:19092` (le port 9092 n'est plus publié sur le poste), pour les consommateurs lancés hors Docker (étape 1.3).
- Tests d'exploration de la Phase 1 dans un projet Gradle autonome `testing/spikes` (JUnit, HTTP contre la pile), lancé avec le wrapper de Fineract : pas de compilation de Fineract, donc compatible avec la pile démarrée sur le poste de 7,4 Go. Hors CI (pile requise).
- Seed de l'étape 1.1 écrit en Python (bibliothèque standard seule) : le cahier ne fixe pas de langage pour `testing/fixtures/seed`. `make seed` est avancé de la phase 2 à l'étape 1.1 ; `make fixtures` reste pour la phase 2.
- Licence : propriétaire, © Circaura Sarl (`LICENSE`) ; composants tiers dans `NOTICE`. Les deux dépôts restent publics.

## Fichiers FICTIFS utilisés

- `deploy/keycloak/fictif-users.json`, `roles.json`, `realm-nkulkap.json` (étape 2.4) : utilisateurs, mots de passe, secrets TOTP et secrets clients FICTIFS du royaume de développement ; permissions par rôle à valider avec les EMF.

- `testing/fixtures/generator/names.json` (étape 2.2) : noms camerounais FICTIFS ; préfixe mobile `+2376999` choisi pour le projet, **[À VÉRIFIER]** : plage réellement réservée aux tests auprès du régulateur.

- `testing/fixtures/seed/seed-data.json` (étape 1.1) : agences, employés, clients et montants FICTIFS ; comptes comptables `FICTIF-xxxx` en attendant le plan comptable des EMF (phase 3).
