# Événements externes de Fineract (Kafka)

Étape 1.3. Comportement observé le 2026-10-22 sur la pile locale (Fineract `b94b2ddf`, Kafka 4.3.2) et figé par `testing/spikes/src/test/java/cm/nkulkap/spikes/ExternalEventsSpikeIT.java` (`make spike-test SPIKE=ExternalEventsSpikeIT` ; pile, `make seed` et jar `fineract-avro-schemas` requis).

## Chaîne de publication

| Élément | Valeur |
|---|---|
| Activation globale | `FINERACT_EXTERNAL_EVENTS_ENABLED=true`, `FINERACT_EXTERNAL_EVENTS_KAFKA_ENABLED=true` (`deploy/compose/env/fineract.env`) |
| Activation par type | `PUT /externalevents/configuration` ; tous les types sont désactivés par défaut. `make seed` active la liste NKULKAP ci-dessous. |
| Stockage | Chaque événement est écrit dans `m_external_event` **dans la transaction métier** : pas d'événement pour une transaction annulée, pas de transaction sans événement. |
| Envoi | Job planifié « Send Asynchronous Events » (`GET /jobs`, id 36), cron `0 0/1 * * * ?` : un lot par minute. |
| Purge | Job « Purge External Events » **inactif** par défaut : `m_external_event` grossit sans fin. À activer en exploitation. |
| Topic | `external-events` (`FINERACT_EXTERNAL_EVENTS_KAFKA_TOPIC_NAME`), clé et valeur en octets |
| Accès depuis le poste | écouteur `HOST` sur `localhost:19092` ajouté à `deploy/compose/dev.yml` (le broker n'annonçait que `kafka:9092`, inaccessible hors du réseau Compose) |

## Format

Valeur Kafka = `org.apache.fineract.avro.MessageV1` sérialisé en Avro binaire (`MessageV1.fromByteBuffer`) :

| Champ | Exemple observé |
|---|---|
| `id` | identifiant de l'événement (long) |
| `source` | identifiant de l'instance Fineract |
| `type` | `SavingsDepositBusinessEvent` |
| `category` | `Savings`, `Loan`, `Client` |
| `createdAt` | `2026-10-22T14:43:02.423344` (UTC, sans fuseau) |
| `businessDate` | `2026-10-22` (date métier, et non date de valeur de la transaction) |
| `tenantId` | `default` |
| `idempotencyKey` | `f1069735-4067-4634-b51a-adf42e2da715` : propre à l'événement, **différente** de l'`Idempotency-Key` de la requête HTTP. Sert à dédupliquer côté consommateur (livraison au moins une fois). |
| `dataschema` | nom complet de la classe Avro de la charge utile, ex. `org.apache.fineract.avro.savings.v1.SavingsAccountTransactionDataV1` |
| `data` | charge utile en Avro binaire : `Class.forName(dataschema).fromByteBuffer(data)` |

Les montants des charges utiles sont des décimaux Avro (`bytes`, `logicalType: decimal`, précision 27, échelle 8) décodés en `BigDecimal` : convertir en entier XAF par `longValueExact()`, jamais par `double`.

## Événements retenus par NKULKAP

| Type | Schéma | Champs utiles |
|---|---|---|
| `ClientCreateBusinessEvent`, `ClientActivateBusinessEvent` | `client.v1.ClientDataV1` | `id`, `accountNo`, `externalId`, `status`, `officeId`, `displayName`, `activationDate`, `timeline` |
| `SavingsDepositBusinessEvent`, `SavingsWithdrawalBusinessEvent`, `SavingsAccountForceWithdrawalBusinessEvent` | `savings.v1.SavingsAccountTransactionDataV1` | `id` (transaction), `accountId`, `accountNo`, `transactionType`, `date`, `amount`, `currency`, `paymentDetailData`, `runningBalance`, `reversed`, `externalId` |
| `LoanDisbursalTransactionBusinessEvent`, `LoanTransactionMakeRepaymentPostBusinessEvent` | `loan.v1.LoanTransactionDataV1` | `id`, `loanId`, `externalLoanId`, `officeId`, `type`, `date`, `amount`, `principalPortion`, `interestPortion`, `feeChargesPortion`, `penaltyChargesPortion`, `outstandingLoanBalance`, `externalId` |
| `LoanBalanceChangedBusinessEvent` | `loan.v1.LoanAccountDataV1` | `id`, `accountNo`, `status`, `clientId`, `clientExternalId`, `summary` (encours, impayés) |

Dans le test, un dépôt de 12 345 XAF produit un `SavingsDepositBusinessEvent` dont `id` = `resourceId` de la réponse HTTP et `amount` = 12 345. Types reçus pendant le test : `ClientCreateBusinessEvent`, `ClientActivateBusinessEvent`, `SavingsDepositBusinessEvent`, `SavingsWithdrawalBusinessEvent`, `LoanDisbursalTransactionBusinessEvent`, `LoanBalanceChangedBusinessEvent`, `LoanTransactionMakeRepaymentPostBusinessEvent` (10 messages).

## Latence

Mesurée entre la réponse HTTP et la réception par le consommateur :

| Action | Latence |
|---|---|
| Création de client | 43,7 s |
| Dépôt d'épargne (appel HTTP : 576 ms) | 58,3 s |
| Retrait | 59,2 s |
| Décaissement | 59,9 s |
| Remboursement | 58,2 s |

La latence suit le cron du job d'envoi (0 à 60 s, plus le temps du lot). Elle se réduit en changeant le cron par `PUT /jobs/36` (ex. `0/5 * * * * ?`), au prix de requêtes plus fréquentes sur `m_external_event`. Pour les besoins temps réel (SMS de confirmation, phase 8), le satellite doit s'appuyer sur la réponse HTTP, pas sur l'événement.

## Écritures comptables : pas d'événement

Aucun événement externe ne porte les écritures des prêts et de l'épargne classiques : le seul type « journal » est `WorkingCapitalLoanJournalEntryCreatedBusinessEvent` (prêts de fonds de roulement). Le cahier attendait un événement d'écriture comptable : **hypothèse fausse**. Options pour la phase 9 (reporting) et `nkulkap-sentinel` :
1. lire `GET /journalentries` par période et par `transactionId` (`S<id>`, `L<id>`), déclenché par les événements de transaction ci-dessus ;
2. publier un événement NKULKAP propre depuis un module custom (point d'extension de l'étape 1.5), sans modifier le cœur.

Le choix relève d'un ADR à rédiger en phase 9, avec la conception du reporting.
