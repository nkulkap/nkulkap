# NKULKAP — Instructions pour Claude Code

## À lire en début de session
1. docs/STATUS.md (où en est-on)
2. L'étape en cours dans docs/specs/NKULKAP_cahier_des_charges_et_plan.md, section 11
3. Les ADR cités par cette étape

Ne relis pas le cahier des charges en entier : seulement l'étape en cours et les sections qu'elle cite.

## Règles d'or
- Fineract = seule vérité comptable. Les satellites postent via API avec Idempotency-Key.
- op_id UUIDv7 créé à la saisie, persisté avant tout réseau, réutilisé à chaque nouvelle tentative.
- Montants XAF en entiers. Jamais de float/double pour l'argent.
- Jamais de modification hors de core/custom-nkulkap dans Fineract, sauf ADR approuvé.
- Tout appel réseau : délai, politique de nouvelle tentative, comportement d'échec.
- Tests d'abord pour les calculs financiers.
- Aucune chaîne d'interface en dur : clés i18n fr + en.
- Donnée réglementaire manquante : utiliser le fichier FICTIF, le noter dans STATUS.md, continuer.
- Code nouveau des apps MPL (nkulkap-field, nkulkap-web) dans des fichiers nouveaux.

## Orchestration des modèles
La session principale (Opus) conçoit, décide et relit. Elle délègue le travail mécanique aux sous-agents de `.claude/agents/` :

| Sous-agent | Modèle | Lui confier |
|---|---|---|
| fineract-explorer | Haiku | Toute recherche dans core/fineract ou apps/* dont seule la conclusion compte |
| nkulkap-boilerplate | Sonnet | CRUD, ressources REST, DTO, migrations simples, config de build, Docker, en suivant un modèle existant |
| nkulkap-i18n | Sonnet | Clés de traduction fr/en, SMS GSM-7, chasse aux chaînes en dur |
| nkulkap-tests | Sonnet | Tests supplémentaires à partir d'un test modèle déjà écrit |

Ne jamais déléguer (reste sur Opus) : calcul du TEG et des intérêts, répartitions Numscript, idempotence et synchronisation hors-ligne (nkulkap-sync, nkulkap-edge), machines à états de nkulkap-pay, règles LAB-FT, sécurité, migrations de données existantes, et le premier test de tout calcul financier.

Procédure :
1. Opus pose le modèle (premier fichier, premier test), puis délègue la suite en indiquant ce modèle au sous-agent.
2. Des tâches indépendantes peuvent partir en parallèle vers plusieurs sous-agents.
3. Opus relit le résultat de chaque sous-agent avant de considérer l'étape comme terminée.
4. Quand une session entière n'est que du travail mécanique, le signaler à l'utilisateur pour qu'il passe sur Sonnet avec /model.

## Commandes
- make bootstrap | up | down | seed | fixtures SIZE=small
- make test COMPONENT=<core|sync|edge|pay|rules|sentinel|field|web>
- make e2e SCENARIO=E2E-01

## Fin d'étape
1. Tous les tests verts (make test COMPONENT=...)
2. Critères « Terminé quand » vérifiés un par un
3. docs/STATUS.md mis à jour (étape, date, écarts, FICTIF utilisés)
4. Commit Conventional Commits : feat(collecte): ...

## En cas de doute
Arrêter et demander plutôt que deviner sur : règles comptables, réglementation, sécurité, suppression de données.
