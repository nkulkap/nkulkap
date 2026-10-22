# ADR-016 — Double garde contre les doublons : `Idempotency-Key` et `externalId` égaux à `op_id`

- Statut : Accepté (validé par l'utilisateur le 2026-10-22)
- Date : 2026-10-22
- Source : étape 1.2, [idempotence-fineract.md](../specs/idempotence-fineract.md)

## Contexte

L'étape 1.2 montre que l'en-tête `Idempotency-Key` de Fineract empêche tout doublon dans les cinq cas du cahier (rejeu, remboursement, envois simultanés, corps différent, redémarrage). Le cahier prévoyait une table de déduplication dans `nkulkap-sync` seulement en cas de doublon : elle n'est pas nécessaire.

Trois limites apparaissent néanmoins :
- la clé est unique par couple action/entité (`DEPOSIT`/`SAVINGSACCOUNT`), pas par compte ni par corps : une clé réutilisée pour une autre opération reçoit la réponse de la première, et la seconde opération est perdue sans erreur ;
- une clé ayant obtenu un refus (4xx) le renvoie à chaque rejeu ;
- si Fineract s'arrête pendant le traitement, la clé reste « en cours » et chaque rejeu reçoit 425, sans fin.

Par ailleurs, l'`externalId` d'une transaction d'épargne (dépôt, retrait, retrait forcé : contrainte `uk_m_sat_external_id`, réponse `403`) ou de prêt (remboursement : validation `value.must.be.unique`, réponse `400`) est unique, quelle que soit la clé d'idempotence. Il se relit par `GET /savingsaccounts/{id}/transactions/external-id/{externalId}` et `GET /loans/{loanId}/transactions/external-id/{externalId}`. Ces comportements sont figés par `IdempotencySpikeIT` (cas h, i, j).

## Décision

1. Tout mouvement posté par un satellite porte `Idempotency-Key` = `op_id` **et** `externalId` = `op_id`. Pas de table de déduplication supplémentaire avant l'appel.
2. Classement des réponses, dans cet ordre :
   - `200` (avec ou sans `x-served-from-cache`) : succès ;
   - `401` : jeton expiré ou invalide ; le satellite renouvelle le jeton puis rejoue avec le même `op_id` ;
   - `400` ou `403` dont le code d'erreur signale une unicité ou une intégrité (`error.msg.data.integrity.issue`, `…externalId.value.must.be.unique`) : le satellite **relit la transaction par `externalId`**. Présente, l'opération est un succès ; absente, c'est un refus définitif. La décision ne repose jamais sur le texte libre de l'erreur (`defaultUserMessage`, message de la base) ;
   - autres `400`, `403`, `404` : refus définitif ; l'opération est close en échec et ne se rejoue pas (la réponse reste en cache sous la clé). Une correction est une nouvelle opération, avec un nouvel `op_id` ;
   - `425`, `5xx`, délai dépassé, absence de réponse : rejeu avec le même `op_id`.
3. Après des `425` répétés sur une durée à fixer en phase 7, le satellite relit par `externalId` : présente, succès ; absente, alerte d'exploitation (clé bloquée « en cours »), sans nouvel envoi automatique sous un autre `op_id`.

## Conséquences

- `nkulkap-sync`, `nkulkap-edge` et `nkulkap-pay` appliquent ces règles ; la phase 7 les met en œuvre et les teste sous Toxiproxy.
- Les `externalId` de transaction sont réservés aux `op_id` : aucune autre source ne doit en écrire.
- L'unicité et la relecture par `externalId` sont vérifiées pour le dépôt, le retrait, le retrait forcé et le remboursement ; elles restent à vérifier pour toute autre commande (transferts, décaissements, création de client, écritures manuelles, dont `referenceNumber` n'est pas un filtre de lecture).
- Le cas « clé bloquée en cours » reste une procédure d'exploitation tant que Fineract ne nettoie pas ces lignes.
- Cette décision dépend du comportement de Fineract `b94b2ddf` : `IdempotencySpikeIT` est relancé à chaque mise à jour de Fineract, et un écart rouvre cet ADR.
