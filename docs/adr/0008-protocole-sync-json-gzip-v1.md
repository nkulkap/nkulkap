# ADR-008 — Protocole de synchronisation : JSON + gzip, versionné (v1)

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Le format reste lisible pour le débogage et gzip est natif sur Android.

## Décision

Protocole de synchronisation en JSON + gzip, versionné (v1).

## Conséquences

Si les mesures dépassent le budget de NFR-04, on passe à Protocol Buffers en v2.
