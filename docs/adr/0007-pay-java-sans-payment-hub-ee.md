# ADR-007 — `nkulkap-pay` en Java, sans Payment Hub EE

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Payment Hub EE épingle Zeebe 8.2.8 (`ph-ee-env-template`), une version ancienne. Depuis Camunda 8.6, l'usage en production de Camunda 8 Self-Managed, Zeebe compris, exige une licence payante. PH-EE impose en plus Kubernetes et Elasticsearch, trop lourds pour une EMF.

## Décision

`nkulkap-pay` est un service Spring Boot avec machine à états en base PostgreSQL, boîte d'envoi transactionnelle (*outbox*) et tâches planifiées.

## Conséquences

L'interface interne de `nkulkap-pay` suit le vocabulaire de la GSMA Mobile Money API pour permettre une intégration ultérieure.
