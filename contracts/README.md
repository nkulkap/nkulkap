# Contrats NKULKAP

Contrats partagés entre les composants (étape 2.1). Validation : `make lint-contracts` (Node ≥ 22.12), exécutée par la CI `ci-contracts`.

| Fichier | Fournisseur | Clients | Références |
|---|---|---|---|
| `sync-v1.openapi.yaml` | `nkulkap-sync` | `nkulkap-field`, `nkulkap-edge` | ADR-008, ADR-016, annexes C et D.2, FR-L5 |
| `pay-v1.openapi.yaml` | `nkulkap-pay` | `nkulkap-sync`, `nkulkap-web`, opérateurs (rappels) | annexe D.1, FR-L6 |
| `sentinel-v1.openapi.yaml` | `nkulkap-sentinel` | `nkulkap-web` | FR-L8 |
| `events/` | émetteurs ci-dessous | `nkulkap-sentinel`, `nkulkap-pay/notify` | FR-L5-09, FR-L6-03, FR-L4-03 |

## Événements (Avro)

Chaque message Kafka contient une enveloppe `common/EventEnvelopeV1` (calquée sur `MessageV1` de Fineract) dont `data` porte la charge utile sérialisée selon le schéma nommé par `dataschema`. Un consommateur décode donc les événements Fineract et NKULKAP de la même façon.

| Type (`type` de l'enveloppe) | Schéma | Émetteur | Topic proposé, clé |
|---|---|---|---|
| `op.posted` | `op/OpPostedV1` | `nkulkap-sync` | `nkulkap.op`, clé `opId` |
| `op.rejected` | `op/OpRejectedV1` | `nkulkap-sync` | `nkulkap.op`, clé `opId` |
| `payment.<état>` (`payment.succeeded`, `payment.posted`…) | `payment/PaymentStateChangedV1` | `nkulkap-pay` | `nkulkap.payment`, clé `intentId` |
| `tournee.soumise`, `tournee.rapprochee`, `tournee.ecart` | `tournee/TourneeStateChangedV1` | module `collecte` | `nkulkap.tournee`, clé `tourneeId` |

Conventions vérifiées par `scripts/validate-events.mjs` :
- espace de noms `cm.nkulkap.events.<domaine>.v<n>`, `doc` sur chaque enregistrement et chaque champ ;
- aucun montant en `float` ou `double` : montants en `long`, unités entières de la devise ;
- un exemple FICTIF `<Schéma>.example.json` par schéma, qui doit passer l'aller-retour d'encodage.

Une évolution incompatible crée un nouveau schéma (`…V2`) ; un champ ajouté porte une valeur par défaut.

## Génération de code

Les clients et serveurs seront générés dans la phase de chaque service, quand il existera : `nkulkap-sync` (Rust, phase 7), `nkulkap-pay` (Java, phase 8), `nkulkap-sentinel` (Scala, phase 10), `nkulkap-field` (Kotlin, phase 7). Aucun code n'est généré à l'étape 2.1.
