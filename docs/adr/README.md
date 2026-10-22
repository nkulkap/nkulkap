# Décisions d'architecture (ADR)

Chaque ADR décrit un contexte, une décision et ses conséquences ; un nouvel ADR prend le numéro suivant, et un ADR remplacé passe au statut « Remplacé par ADR-0xx ».

| Numéro | Titre | Statut |
|---|---|---|
| ADR-001 | [Fineract est la seule source de vérité comptable](0001-fineract-seule-verite-comptable.md) | Accepté |
| ADR-002 | [Le code NKULKAP côté Fineract vit dans `custom/nkulkap/<domaine>/<module>`](0002-code-nkulkap-custom-nkulkap.md) | Accepté |
| ADR-003 | [Le hors-bilan se gère par des comptes étiquetés, sans modifier `GLAccountType`](0003-hors-bilan-comptes-etiquetes.md) | Accepté |
| ADR-004 | [Rust pour `nkulkap-sync` et `nkulkap-edge`](0004-rust-sync-et-edge.md) | Accepté |
| ADR-005 | [Numscript pour les répartitions, exécuté par un adaptateur Go sans état](0005-numscript-adaptateur-go-sans-etat.md) | Accepté |
| ADR-006 | [Scala 3 pour `nkulkap-sentinel`](0006-scala-3-sentinel.md) | Accepté |
| ADR-007 | [`nkulkap-pay` en Java, sans Payment Hub EE](0007-pay-java-sans-payment-hub-ee.md) | Accepté |
| ADR-008 | [Protocole de synchronisation : JSON + gzip, versionné (v1)](0008-protocole-sync-json-gzip-v1.md) | Accepté |
| ADR-009 | [PostgreSQL comme base unique, Apache Kafka en mode KRaft](0009-postgresql-kafka-kraft.md) | Accepté |
| ADR-010 | [Keycloak (OIDC) pour l'identité](0010-keycloak-oidc-identite.md) | Accepté |
| ADR-011 | [Hébergement au Cameroun par défaut](0011-hebergement-cameroun-par-defaut.md) | Accepté |
| ADR-012 | [Modèle de multi-location](0012-modele-multi-location.md) | Accepté |
| ADR-013 | [Jonction NTFS pour relier les modules custom sous Windows](0013-jonction-ntfs-modules-custom-windows.md) | Accepté |
| ADR-014 | [Intégration continue sur GitHub Actions, avec bascule prévue vers TeamCity](0014-ci-github-actions-bascule-teamcity.md) | Accepté |
| ADR-015 | [Conventions Gradle propres aux modules NKULKAP : formatage et en-tête de licence](0015-conventions-gradle-modules-nkulkap.md) | Accepté |
| ADR-016 | [Double garde contre les doublons : `Idempotency-Key` et `externalId` égaux à `op_id`](0016-double-garde-idempotency-key-et-externalid.md) | Accepté |
| ADR-017 | [Écritures comptables : pas d'événement externe Fineract, lecture par l'API](0017-ecritures-comptables-sans-evenement-externe.md) | Accepté |
