# ADR-012 — Modèle de multi-location

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Fineract est multi-locataire, avec une base par locataire.

## Décision

Une petite EMF partage l'instance SaaS. Une grande EMF ou une banque dispose de sa propre instance.

## Conséquences

Tous les satellites portent l'identifiant de locataire (`Fineract-Platform-TenantId`).
