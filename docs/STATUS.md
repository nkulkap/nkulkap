# NKULKAP — Avancement

Dernière mise à jour : 2026-10-22. Plan détaillé : [section 11 du cahier](specs/NKULKAP_cahier_des_charges_et_plan.md), une fiche par phase dans [specs/sections/](specs/README.md).

## Phases

Modèle conseillé : **Opus** conçoit, écrit la logique critique et relit ; **Sonnet** fait le travail mécanique via les sous-agents de `.claude/agents/` ; **Haiku** explore le code (`fineract-explorer`). Ce qui ne se délègue jamais est listé dans `CLAUDE.md`.

| Phase | Contenu | Statut | Modèle conseillé |
|---|---|---|---|
| 0 | Préparer : dépôts, outillage, pile de développement | En cours (0.1, 0.2 et 0.3 faites ; 0.4 et 0.5 à faire) | Sonnet pour squelette, ADR, Compose, CI ; Opus pour Makefile, choix du fork, relecture |
| 1 | Comprendre : explorations et vérifications d'hypothèses | À faire | Opus pilote et conclut ; Haiku (`fineract-explorer`) pour les recherches |
| 2 | Socle commun : contrats, données de test, réseau dégradé, identité | À faire | Opus pour contrats et identité (sécurité) ; Sonnet pour fixtures FICTIVES, profils Toxiproxy, Keycloak en suivant le modèle |
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
| 0.2 Fork Fineract et modules custom | Fait (2026-10-22) | Fork public `nkulkap/nkulkap-fineract` (branche par défaut `nkulkap/main` = `b94b2ddf`), sous-module `core/fineract`, module `core/custom-nkulkap/common/core` (calqué sur `custom/acme/note/service`), jonction `core/fineract/custom/nkulkap` (ADR-013). `./gradlew projects` liste `:custom:nkulkap:common:core` ; `settings.gradle` identique à `b94b2ddf` ; `./gradlew :custom:nkulkap:common:core:build -x spotlessGroovyGradle -x spotlessCheck` : BUILD SUCCESSFUL. Réserve : `spotless` non vérifié, `download.eclipse.org` étant en arrêt temporaire le 2026-10-22 ; à relancer sans `-x`. |
| 0.3 ADR | Fait | 13 fichiers dans `docs/adr/` (001 à 012 du cahier, 013 pour la jonction Windows), tous liés depuis `docs/adr/README.md`. |
| 0.4 Pile de développement | Bloquée | Dépend de la compilation de 0.2 (image `fineract-custom`) ; démon Docker injoignable sur le poste. |
| 0.5 Intégration continue | À faire | Dépôts distants créés ; reste à écrire les workflows et les vérifier sur GitHub. |

### Poste de développement (Windows, 7,4 Go de RAM)

- JDK 25 (`Microsoft.OpenJDK.25`) installé ; GNU Make toujours absent.
- Docker Desktop (WSL2) limité à 5 Go par `%USERPROFILE%\.wslconfig` (`memory=5GB`, `swap=8GB`).
- `~/.gradle/gradle.properties` ramène Gradle à `-Xmx3g`, 2 workers, sans parallélisme : le `-Xmx12g` de Fineract fait planter la JVM sur ce poste.
- Le sous-module Fineract doit être extrait en LF (`core.autocrlf=false`) : en CRLF, le build réécrit les sources et échoue. `make bootstrap` le règle.
- Ne pas compiler Fineract pendant que la pile Docker tourne (mémoire insuffisante pour les deux).

### Dépôts

- Monodépôt : https://github.com/nkulkap/nkulkap (public).
- Fork : https://github.com/nkulkap/nkulkap-fineract (public, fork d'`apache/fineract`). Base `b94b2ddf` plutôt que l'étiquette `1.16.0`, car celle-ci (19/10/2026) est antérieure au commit de référence (20/10/2026), conformément à la règle de l'étape 0.2.
- Sous Git pour Windows, cloner Fineract avec `-c http.version=HTTP/1.1 -c core.longpaths=true` : en HTTP/2, le clone échoue (`curl 92`, `early EOF`).

## Écarts

- Projet renommé NKULAP → NKULKAP. Le cahier a été copié dans `docs/specs/` avec le renommage appliqué ; l'original reste dans Téléchargements.
- Dans la copie, la phrase sur la recherche web d'antériorité précise désormais qu'elle portait sur « Nkulap ». Elle est marquée **[À VÉRIFIER]** : refaire la recherche pour « Nkulkap ».
- Le cahier est découpé en 44 fiches dans `docs/specs/sections/`, générées par `scripts/split-spec.sh`. La copie complète reste la référence.
- `apps/` n'existe pas encore : ses deux dossiers seront des sous-modules (forks openMF).
- Liaison des modules custom sous Windows par jonction NTFS plutôt que par lien symbolique ou copie (ADR-013).
- Groupe Gradle `cm.nkulkap.fineract` choisi par analogie avec `com.acme.fineract` : le cahier ne fixe pas de nom de paquet.
- L'audit Apache RAT est désactivé dans les modules NKULKAP : il exige l'en-tête de licence de la fondation Apache, qui ne s'applique pas à notre code. À revoir quand la licence sera choisie.
- Les dépôts sont publics et le monodépôt n'a pas encore de fichier `LICENSE` : sans licence, le code reste « tous droits réservés ». Licence à choisir (voir section 9.3 du cahier).

## Fichiers FICTIFS utilisés

Aucun.
