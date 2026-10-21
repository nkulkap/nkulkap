# ADR-004 — Rust pour `nkulkap-sync` et `nkulkap-edge`

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Constat d'audit : l'app `openMF/android-client` envoie chaque opération hors-ligne directement à l'API Fineract, sans clé d'idempotence (0 occurrence dans le code Kotlin). En cas d'erreur, elle garde l'opération et la renvoie. Une réponse perdue après enregistrement provoque donc un doublon. Il faut une passerelle qui garantisse l'exactement-une-fois, et un nœud d'agence qui tourne des jours sur du matériel modeste sans surveillance.

## Décision

Rust pour `nkulkap-sync` et `nkulkap-edge`.

## Conséquences

Rust apporte : binaire statique unique (cible `musl`, x86_64 et aarch64), empreinte mémoire faible et stable, pas de ramasse-miettes, sûreté mémoire, concurrence sûre pour des files de messages critiques.
