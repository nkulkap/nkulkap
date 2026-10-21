# ADR-003 — Le hors-bilan se gère par des comptes étiquetés, sans modifier `GLAccountType`

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

`GLAccountType` ne connaît que 5 types (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE), dans `fineract-core`.

## Décision

Les comptes de classe 9 du PCEMF sont créés comme paires de comptes ASSET et LIABILITY qui se compensent. Ils portent l'étiquette `HORS_BILAN`, via les valeurs de code d'étiquettes de comptes de Fineract.

## Conséquences

Ces comptes sont exclus du bilan par le moteur de reporting (L7). Un changement du cœur pourra être proposé en amont plus tard.
