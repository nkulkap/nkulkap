# ADR-002 — Le code NKULKAP côté Fineract vit dans `custom/nkulkap/<domaine>/<module>`

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Fineract inclut automatiquement les modules qui suivent le motif `custom/<société>/<catégorie>/<module>` (voir `settings.gradle`). Limite connue : la documentation de Fineract qualifie encore les modules personnalisés de « proof of concept ».

## Décision

Le code NKULKAP côté Fineract vit dans `custom/nkulkap/<domaine>/<module>`. Ses migrations vont dans `db/custom-changelog`. Ses ressources REST sont des beans annotés `@Path`, enregistrés par `JerseyConfig`.

## Conséquences

Les modules NKULKAP ajoutent des fonctions nouvelles plutôt que de remplacer des services existants.
