# Idempotence des commandes Fineract

Étape 1.2. Comportement observé le 2026-10-22 sur la pile locale (Fineract `b94b2ddf`, PostgreSQL 18.3) et figé par `testing/spikes/src/test/java/cm/nkulkap/spikes/IdempotencySpikeIT.java` (`make spike-test SPIKE=IdempotencySpikeIT`, pile démarrée et `make seed` passé). Conséquences pour NKULKAP : [ADR-016](../adr/0016-double-garde-idempotency-key-et-externalid.md).

**Résultat : une seule transaction existe dans chacun des cas (a) à (e) exigés.** Aucun doublon.

## Mécanisme (lecture du code)

| Élément | Emplacement | Effet |
|---|---|---|
| Lecture de l'en-tête | `IdempotencyStoreFilter` (`fineract-core/.../infrastructure/core/filters/`) | `Idempotency-Key` (nom réglable par `FINERACT_IDEMPOTENCY_KEY_HEADER_NAME`) n'est retenue que si la requête est `application/json`. |
| Clé absente | `IdempotencyKeyResolver` | Fineract génère une clé aléatoire : aucune protection contre le rejeu. |
| Registre | table `m_portfolio_command_source`, colonnes `action_name`, `entity_name`, `idempotency_key`, `status`, `result`, `result_status_code` | Une ligne par commande, insérée **avant** le traitement (`status` = 4, en cours), dans sa propre transaction. |
| Unicité | contrainte `UNIQUE_PORTFOLIO_COMMAND_SOURCE (action_name, entity_name, idempotency_key)` (changelog `0061`) | La clé est unique **par couple action/entité** (`DEPOSIT`/`SAVINGSACCOUNT`, `REPAYMENT`/`LOAN`…), pas par compte ni par corps. |
| Contrôle avant traitement | `SynchronousCommandProcessingService.exceptionWhenTheRequestAlreadyProcessed` | `PROCESSED` → réponse en cache ; `ERROR` → erreur en cache ; `UNDER_PROCESSING` → 425. |
| Nouvelle tentative côté serveur | `fineract.retry.instances.executeCommand` : 3 tentatives, attente 1 s puis 2 s ; inclut `IdempotentCommandProcessUnderProcessingException` | Un envoi concurrent attend que le premier se termine (≈ 3 s au plus) avant de répondre 425. |
| Réponse rejouée | `IdempotentCommandExceptionMapper` | Statut et corps d'origine, plus l'en-tête `x-served-from-cache: true`. |

## Cas exigés

| Cas | Envois | Réponses observées | Transactions |
|---|---|---|---|
| (a) même dépôt d'épargne ×2, même clé | `POST /savingsaccounts/{id}/transactions?command=deposit` | 1er : `200` `{"officeId":1,"clientId":11,"savingsId":11,"resourceId":11,"changes":{"paymentTypeId":4}}`. 2e : `200`, `x-served-from-cache: true`, **corps identique** (même `resourceId`). | 1 dépôt, solde 10 000, 2 lignes d'écriture |
| (b) même remboursement de prêt ×2 | `POST /loans/{id}/transactions?command=repayment` | 1er : `200` `{"loanId":2,"resourceId":5,"changes":{...}}`. 2e : `200`, `x-served-from-cache: true`, corps identique. | 1 remboursement, total remboursé 20 000 |
| (c) 4 envois simultanés, même clé, 10 tours | 4 fils libérés ensemble | À chaque tour : un `200` sans en-tête de cache, trois `200` avec `x-served-from-cache: true`. Aucun 425 observé : les envois concurrents sont mis en attente par la nouvelle tentative serveur. | 1 dépôt par tour |
| (d) même clé, corps différent (10 000 puis 25 000) | dépôt | 2e : `200`, `x-served-from-cache: true`, réponse du **premier** envoi. Fineract ne compare pas les corps : le second montant est ignoré sans erreur. | 1 dépôt de 10 000 |
| (e) rejeu après redémarrage de Fineract | `docker compose restart fineract` (≈ 223 s), puis même envoi | `200`, `x-served-from-cache: true`, corps identique : le registre est en base, il survit au redémarrage. | 1 dépôt |

## Cas complémentaires (risques pour `nkulkap-sync`)

| Cas | Observation | Risque |
|---|---|---|
| (d2) même clé sur un **autre compte** | Le dépôt sur le compte B reçoit `200` en cache avec `"savingsId"` du compte A ; rien n'est déposé sur B. | **Perte silencieuse** si une clé est réutilisée pour une autre opération de même type. |
| (f) envoi refusé (`400`, date future), puis corrigé avec la même clé | Le corrigé reçoit le `400` d'origine, `x-served-from-cache: true` ; avec une clé neuve : `200`. | Une clé ayant obtenu un 4xx est « brûlée ». |
| (g) aucun en-tête | Deux envois → deux dépôts (`resourceId` 25 et 26). | Doublon : la clé est indispensable. |
| (h) clé neuve mais même `externalId` de transaction | Dépôt : `403` `error.msg.data.integrity.issue` (contrainte `uk_m_sat_external_id`). Remboursement : `400` `value.must.be.unique`. Une seule transaction ; lecture `GET /savingsaccounts/{id}/transactions/external-id/{externalId}`, et `resourceExternalId` dans la réponse d'un remboursement. | Aucun : seconde garde, indépendante de la clé. |
| Arrêt brutal pendant le traitement (non testé, lecture du code) | La ligne du registre est validée avant le traitement avec `status` = en cours. Si Fineract s'arrête avant la fin, la transaction métier est annulée mais la ligne reste « en cours » ; aucun code ne la nettoie. Tout rejeu avec cette clé reçoit alors 425 indéfiniment. | Blocage sans doublon : le client doit pouvoir conclure autrement (voir ADR-016). |

## Règles pour les satellites

1. Toujours envoyer `Idempotency-Key` = `op_id` (UUIDv7 créé à la saisie) et `Content-Type: application/json`.
2. Mettre aussi `externalId` = `op_id` sur la transaction (dépôt, retrait, remboursement) : c'est la garde qui ne dépend pas du couple action/entité.
3. `200` (avec ou sans `x-served-from-cache`) : succès définitif ; lire `resourceId`.
4. `400`/`403` : refus définitif ; ne pas rejouer avec la même clé (réponse en cache). Une correction est une nouvelle opération (nouvel `op_id`).
5. `425`, `5xx`, délai dépassé : rejouer avec la même clé. Après plusieurs `425` étalés, vérifier l'existence par `externalId` avant de conclure.
6. Ne jamais réutiliser une clé pour une autre opération, même sur un autre compte.
