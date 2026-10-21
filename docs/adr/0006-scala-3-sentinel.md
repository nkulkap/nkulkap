# ADR-006 — Scala 3 pour `nkulkap-sentinel`

- Statut : Accepté
- Date : 2026-10-21
- Source : cahier des charges, section 3

## Contexte

Fineract publie ses événements métier sur Kafka au format Avro (schémas dans `fineract-avro-schemas`).

## Décision

Scala 3, avec Cats Effect et fs2-kafka, pour `nkulkap-sentinel`.

## Conséquences

Ce choix convient au traitement de flux à états (fenêtres glissantes, vélocité, fractionnement) et à l'expression de règles sous forme de fonctions pures testables par propriétés.
