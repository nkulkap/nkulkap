# NKULKAP — Avancement

Dernière mise à jour : 2026-10-21. Plan détaillé : [section 11 du cahier](specs/NKULKAP_cahier_des_charges_et_plan.md), une fiche par phase dans [specs/sections/](specs/README.md).

## Phases

Modèle conseillé : **Opus** conçoit, écrit la logique critique et relit ; **Sonnet** fait le travail mécanique via les sous-agents de `.claude/agents/` ; **Haiku** explore le code (`fineract-explorer`). Ce qui ne se délègue jamais est listé dans `CLAUDE.md`.

| Phase | Contenu | Statut | Modèle conseillé |
|---|---|---|---|
| 0 | Préparer : dépôts, outillage, pile de développement | En cours (0.1 et 0.3 faites, 0.2, 0.4 et 0.5 bloquées) | Sonnet pour squelette, ADR, Compose, CI ; Opus pour Makefile, choix du fork, relecture |
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
| 0.2 Fork Fineract et modules custom | Bloquée | Décisions humaines requises, voir plus bas. |
| 0.3 ADR | Fait | 12 fichiers dans `docs/adr/`, tous liés depuis `docs/adr/README.md`. |
| 0.4 Pile de développement | Bloquée | Dépend de 0.2 (image `fineract-custom`) ; démon Docker injoignable sur le poste. |
| 0.5 Intégration continue | Bloquée | Exige un dépôt distant GitHub pour vérifier le déclenchement par chemin. |

### Bloquants (décision ou action humaine)

1. **Fork `nkulkap-fineract`** : compte ou organisation GitHub où le créer. L'amont a publié l'étiquette `1.16.0`, postérieure à la `1.15.0` citée par le cahier : choisir la base entre le commit `b94b2ddf` et `1.16.0`.
2. **Dépôt distant du monodépôt** `nkulkap` (nécessaire à 0.5).
3. **Outillage du poste** : installer GNU Make et un JDK 21 (seul Java 11 est présent) ; démarrer Docker Desktop.
4. **Lien symbolique sous Windows** : activer le mode développeur, ou utiliser `make bootstrap LINK_MODE=copy` et consigner ce choix dans un ADR comme le prévoit l'étape 0.2.

## Écarts

- Projet renommé NKULAP → NKULKAP. Le cahier a été copié dans `docs/specs/` avec le renommage appliqué ; l'original reste dans Téléchargements.
- Dans la copie, la phrase sur la recherche web d'antériorité précise désormais qu'elle portait sur « Nkulap ». Elle est marquée **[À VÉRIFIER]** : refaire la recherche pour « Nkulkap ».
- Le cahier est découpé en 44 fiches dans `docs/specs/sections/`, générées par `scripts/split-spec.sh`. La copie complète reste la référence.
- `apps/` n'existe pas encore : ses deux dossiers seront des sous-modules (forks openMF).

## Fichiers FICTIFS utilisés

Aucun.
