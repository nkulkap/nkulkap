<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1244 à 1281. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 14. Annexes

### Annexe A — Gabarit de `CLAUDE.md`

```markdown
# NKULKAP — Instructions pour Claude Code

## À lire en début de session
1. docs/STATUS.md (où en est-on)
2. L'étape en cours dans docs/specs/NKULKAP_cahier_des_charges_et_plan.md, section 11
3. Les ADR cités par cette étape

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
```

