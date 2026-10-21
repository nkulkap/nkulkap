# ADR-005 — Numscript pour les répartitions, exécuté par un adaptateur Go sans état

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Numscript (licence MIT, `github.com/formancehq/numscript`) est une bibliothèque Go. Fineract reste la seule comptabilité (ADR-001).

## Décision

`nkulkap-rules` est un service Go minimal, de l'ordre de 300 lignes de code applicatif, qui charge des scripts `.num`, les exécute avec la source `allowing unbounded overdraft` (calcul pur, sans solde) et renvoie des écritures. On n'utilise jamais le ledger Formance.

## Conséquences

La logique métier vit dans les fichiers `.num`, lisibles et modifiables sans recompiler. Exemples validés en annexe B.
