# ADR-001 — Fineract est la seule source de vérité comptable

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Fineract prend en charge nativement l'idempotence : voir `IdempotencyKeyResolver` et la propriété `fineract.idempotency-key-header-name`.

## Décision

Fineract est la seule source de vérité comptable. Les satellites écrivent dans Fineract par son API REST, avec l'en-tête `Idempotency-Key`.

## Conséquences

Les satellites (`nkulkap-sync`, `nkulkap-pay`, `nkulkap-rules`, `nkulkap-sentinel`, `nkulkap-edge`) ne tiennent aucun solde officiel. Toute écriture comptable passe par l'API REST de Fineract avec une clé d'idempotence.
