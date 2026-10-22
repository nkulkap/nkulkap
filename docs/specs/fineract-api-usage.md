# Usage de l'API Fineract par NKULKAP

Étape 1.1. Comportements observés le 2026-10-22 sur la pile locale (`deploy/compose/dev.yml`, Fineract `b94b2ddf`), avec le jeu FICTIF de `testing/fixtures/seed` (`make seed`). Les corps cités sont des extraits de réponses réelles, raccourcis.

## Conventions communes

| Point | Valeur observée |
|---|---|
| Base | `https://<hôte>:8443/fineract-provider/api/v1` (TLS autosigné en local) |
| En-têtes | `Authorization: Basic …` (jeu FICTIF : `mifos`/`password`), `Fineract-Platform-TenantId: default`, `Content-Type: application/json` |
| Dates | Chaînes, avec `"dateFormat": "yyyy-MM-dd"` et `"locale": "fr"` dans le corps. En lecture, les dates reviennent en tableau `[2026, 1, 2]`. |
| Montants | Envoyés en entiers XAF (`"transactionAmount": 150000`). Lus en décimaux (`150000.0`) : à convertir en entier côté satellite, jamais en `double` pour un calcul. |
| Devise | XAF doit figurer dans `PUT /currencies` (devises de l'organisation), sinon `POST /journalentries` répond 404 `error.msg.currency.currencyCode.invalid.or.not.supported`. La création de produits XAF, elle, n'est pas bloquée. Le seed l'ajoute. |
| Idempotence | En-tête `Idempotency-Key` sur toute commande (POST/PUT). Rejeu : réponse mise en cache, en-tête `x-served-from-cache: true`. Comportement complet : [idempotence-fineract.md](idempotence-fineract.md) (étape 1.2). |
| Réponse d'une commande | `{"officeId", "clientId", "savingsId"|"loanId", "resourceId", "changes", "resourceExternalId"?}` ; `resourceId` = identifiant de la transaction créée. |

### Forme des erreurs

Toutes les erreurs métier ont la même enveloppe :

```json
{"developerMessage": "...", "httpStatusCode": "403", "defaultUserMessage": "...",
 "userMessageGlobalisationCode": "validation.msg.domain.rule.violation",
 "errors": [{"developerMessage": "Insufficient account balance.",
             "userMessageGlobalisationCode": "error.msg.savingsaccount.transaction.insufficient.account.balance",
             "parameterName": "id", "args": [...]}]}
```

| Code | Sens | Exemple observé |
|---|---|---|
| 400 | Validation des paramètres | montant ≤ 0, `paymentTypeId` manquant, `command` inconnue, date de dépôt future |
| 403 | Règle métier ou intégrité | solde insuffisant ; débits ≠ crédits ; doublon de `glCode` (`error.msg.glAccount.unknown.data.integrity.issue`) ; remboursement à date future |
| 404 | Ressource absente | prêt, rapport ou devise inconnus |
| 425 | Même `Idempotency-Key` en cours de traitement | voir étape 1.2 |
| 500 | Erreur non traitée | `PUT /externalevents/configuration` avec un type d'événement inconnu |

Les codes `userMessageGlobalisationCode` sont stables : les satellites les mappent vers leurs clés i18n fr/en, jamais le texte `defaultUserMessage`.

Note : une même erreur fonctionnelle n'a pas toujours le même code (date future : 400 sur un dépôt d'épargne, 403 sur un remboursement de prêt). Un satellite traite donc 400 et 403 comme « refus définitif, ne pas rejouer », et 5xx ou absence de réponse comme « rejouer avec la même clé ».

## Comptabilité

### `GET /glaccounts`

Filtres utiles : `type` (1 actif, 2 passif, 3 capitaux, 4 produit, 5 charge), `usage` (1 détail, 2 en-tête), `disabled`, `manualEntriesAllowed`. Pas de filtre par `glCode` : NKULKAP lit la liste et indexe par `glCode`.

```json
[{"id": 1, "name": "FICTIF Caisse agence", "glCode": "FICTIF-5711", "disabled": false,
  "manualEntriesAllowed": true, "type": {"id": 1, "value": "ASSET"},
  "usage": {"id": 1, "value": "DETAIL"}, "tagId": {"id": 0}}]
```

`GET /glaccounts/{id}` : même objet.

### `POST /glaccounts`

```json
{"name": "FICTIF Caisse agence", "glCode": "FICTIF-5711", "type": 1, "usage": 1, "manualEntriesAllowed": true}
```

→ 200 `{"resourceId": 1}`. Erreurs : 400 `validation.msg.GLAccount.type.is.not.within.expected.range`, `...manualEntriesAllowed.cannot.be.blank` ; 403 sur `glCode` existant (contrainte `acc_gl_account_gl_code_key`). `DELETE /glaccounts/{id}` supprime un compte sans écriture. Le hors-bilan passera par des étiquettes (`tagId`), ADR-003.

### `POST /journalentries` (écriture manuelle)

```json
{"officeId": 2, "transactionDate": "2026-01-03", "currencyCode": "XAF", "comments": "FICTIF apport",
 "referenceNumber": "PROBE-JE-1",
 "debits": [{"glAccountId": 1, "amount": 1000}], "credits": [{"glAccountId": 8, "amount": 1000}],
 "dateFormat": "yyyy-MM-dd", "locale": "fr"}
```

→ 200 `{"officeId": 2, "transactionId": "a30b07450a0c"}`. Erreurs : 403 `error.msg.glJournalEntry.invalid.mismatch.debits.credits` ; 404 devise non déclarée.

### `GET /journalentries`

Filtres : `officeId`, `glAccountId`, `fromDate`/`toDate` (avec `dateFormat`/`locale`), `transactionId`, `entityType`, `manualEntriesOnly`, `loanId`, `savingsId`, `limit`/`offset`. **`referenceNumber` n'est pas un filtre** : `?referenceNumber=…` est ignoré et renvoie toutes les écritures. Une vérification de doublon par `referenceNumber` est donc impossible par cette route (à retenir pour l'étape 1.2).

Les écritures automatiques portent `transactionId` = `S<id>` (épargne) ou `L<id>` (prêt), `entityType` `SAVING`/`LOAN`, `entityId` = compte :

```json
{"totalFilteredRecords": 2, "pageItems": [{"id": 1, "officeId": 2, "glAccountCode": "FICTIF-5711",
  "entryType": {"value": "DEBIT"}, "amount": 150000.0, "currency": {"code": "XAF", "decimalPlaces": 0},
  "transactionId": "S1", "manualEntry": false, "entityType": {"value": "SAVING"}, "entityId": 1}]}
```

## Épargne : `POST /savingsaccounts/{id}/transactions?command=…`

Commandes acceptées (liste renvoyée par l'erreur `command` inconnue) : `deposit`, `withdrawal`, `force-withdrawal`, `holdAmount`. Lecture : `GET /savingsaccounts/{id}/transactions/{txId}` ; compte par identifiant externe : `GET /savingsaccounts/external-id/{externalId}` (`?associations=transactions` pour l'historique).

### `deposit`

```json
{"transactionDate": "2026-01-05", "transactionAmount": 1000, "paymentTypeId": 1,
 "dateFormat": "yyyy-MM-dd", "locale": "fr"}
```

→ 200 `{"officeId": 2, "clientId": 2, "savingsId": 2, "resourceId": 11, "changes": {"paymentTypeId": 1}}`. Écritures (produit en comptabilité de caisse) : débit caisse (`savingsReferenceAccountId`), crédit dépôts de la clientèle (`savingsControlAccountId`). Erreurs : 400 montant ≤ 0 (`...transactionAmount.not.greater.than.zero`), 400 date future (`error.msg.savingsaccount.transaction.in.the.future`), 400 `paymentTypeId` manquant.

### `withdrawal`

Même corps. → 200, même forme de réponse. Erreur : 403 `error.msg.savingsaccount.transaction.insufficient.account.balance`.

### `force-withdrawal`

Même corps. Sans configuration particulière, il est refusé comme un retrait ordinaire (403 solde insuffisant). Conditions d'activation, permission et écritures : [force-withdrawal.md](force-withdrawal.md) (étape 1.8).

## Prêts : `POST /loans/{id}/transactions?command=repayment`

Modèle : `GET /loans/{id}/transactions/template?command=repayment` (montant dû, date). Prêt par identifiant externe : `GET /loans/external-id/{externalId}`.

```json
{"transactionDate": "2026-02-02", "transactionAmount": 10000, "paymentTypeId": 1,
 "externalId": "PROBE-RMB-0d8c97e1", "dateFormat": "yyyy-MM-dd", "locale": "fr"}
```

→ 200 `{"officeId": 2, "clientId": 3, "loanId": 1, "resourceId": 3, "changes": {...}, "resourceExternalId": "PROBE-RMB-0d8c97e1"}`. L'`externalId` de transaction est unique et renvoyé : candidat pour une vérification de doublon côté prêts. Erreurs : 403 `error.msg.loan.transaction.cannot.be.a.future.date` ; 404 `error.msg.loan.id.invalid`.

## Rapports : `GET /runreports/{nom}`

Liste : `GET /reports?fields=id,reportName,reportType`. Exécution : `GET /runreports/Client%20Listing?R_officeId=1&genericResultSet=false` ; paramètres préfixés `R_`. Avec `genericResultSet=false`, tableau d'objets par libellé de colonne :

```json
[{"Office/Branch": "..FICTIF Agence Douala Akwa", "Client Account No.": "000000001",
  "name": "Abena FICTIF-Manga", "Status": "Active", "Activation": [2026, 1, 2], "External Id": "SEED-CLI-01"}]
```

Sans ce paramètre : `{"columnHeaders": [...], "data": [{"row": [...]}]}`. Erreur : 404 `error.msg.report.name.not.found`. Les états réglementaires (phase 9) seront des rapports déclarés ici.

## Événements externes : `/externalevents/configuration`

`GET` → `{"externalEventConfiguration": [{"type": "ClientCreateBusinessEvent", "enabled": false}, ...]}` (tous désactivés par défaut).

`PUT` :

```json
{"externalEventConfigurations": {"ClientCreateBusinessEvent": true}}
```

→ 200 `{"changes": {"externalEventConfigurations": {"ClientCreateBusinessEvent": true}}}`. Un type inconnu donne **500** sans enveloppe d'erreur : vérifier les noms contre le `GET` avant d'écrire. Événements activés par NKULKAP : étape 1.3, [fineract-events.md](fineract-events.md).

## Endpoints utilisés par le seed

| Endpoint | Usage | Rejouabilité |
|---|---|---|
| `GET/PUT /currencies` | ajouter XAF | ajout seulement si absente |
| `GET/POST /offices` | 2 agences (`parentId` 1, `externalId`) | recherche par `externalId` dans la liste |
| `GET /staff?status=all`, `POST /staff` | 3 employés | idem |
| `GET/POST /glaccounts` | 14 comptes FICTIFS | recherche par `glCode` |
| `GET/POST /paymenttypes` | type « espèces » (`isCashPayment`) | réutilise le premier existant |
| `GET/POST /savingsproducts` | produit d'épargne XAF, comptabilité de caisse (`accountingRule` 2) | recherche par nom |
| `GET/POST /loanproducts` | produit de prêt XAF, 6 mensualités, comptabilité de caisse | recherche par nom |
| `GET /clients?externalId=`, `POST /clients` | 10 clients actifs | filtre `externalId` |
| `GET /savingsaccounts/external-id/{e}`, `POST /savingsaccounts`, `?command=approve`, `?command=activate` | 10 comptes | 404 = absent |
| `POST /savingsaccounts/{id}/transactions?command=deposit` | dépôt initial | `Idempotency-Key` stable (UUIDv5 du nom du mouvement) |
| `GET /loans/external-id/{e}`, `POST /loans`, `?command=approve`, `?command=disburse` | 1 prêt décaissé | 404 = absent ; décaissement sauté si actif, sinon clé stable |
