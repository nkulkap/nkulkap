# ADR-016 — Double garde contre les doublons : `Idempotency-Key` et `externalId` égaux à `op_id`

- Statut : Proposé
- Date : 2026-10-22
- Source : étape 1.2, [idempotence-fineract.md](../specs/idempotence-fineract.md)

## Contexte

L'étape 1.2 montre que l'en-tête `Idempotency-Key` de Fineract empêche tout doublon dans les cinq cas du cahier (rejeu, remboursement, envois simultanés, corps différent, redémarrage). Le cahier prévoyait une table de déduplication dans `nkulkap-sync` seulement en cas de doublon : elle n'est pas nécessaire.

Trois limites apparaissent néanmoins :
- la clé est unique par couple action/entité (`DEPOSIT`/`SAVINGSACCOUNT`), pas par compte ni par corps : une clé réutilisée pour une autre opération reçoit la réponse de la première, et la seconde opération est perdue sans erreur ;
- une clé ayant obtenu un refus (4xx) le renvoie à chaque rejeu ;
- si Fineract s'arrête pendant le traitement, la clé reste « en cours » et chaque rejeu reçoit 425, sans fin.

Par ailleurs, l'`externalId` d'une transaction d'épargne ou de prêt est unique (contrainte `uk_m_sat_external_id` ; validation `value.must.be.unique`) et se relit par `GET /savingsaccounts/{id}/transactions/external-id/{externalId}`.

## Décision

1. Tout mouvement posté par un satellite porte `Idempotency-Key` = `op_id` **et** `externalId` = `op_id`. Pas de table de déduplication supplémentaire avant l'appel.
2. Classement des réponses : `200` = succès (avec ou sans `x-served-from-cache`) ; `400`/`403` = refus définitif, l'opération est close en échec et ne se rejoue pas ; `425`, `5xx`, délai dépassé = rejeu avec le même `op_id`.
3. Un `403` dont `defaultUserMessage` cite la contrainte d'`externalId` (ou un `400` `value.must.be.unique`) signifie « déjà enregistré » : le satellite relit la transaction par `externalId` et conclut au succès.
4. Après des `425` répétés sur une durée à fixer en phase 7, le satellite relit par `externalId` : présente, succès ; absente, alerte d'exploitation (clé bloquée « en cours »), sans nouvel envoi automatique sous un autre `op_id`.

## Conséquences

- `nkulkap-sync`, `nkulkap-edge` et `nkulkap-pay` appliquent ces règles ; la phase 7 les met en œuvre et les teste sous Toxiproxy.
- Les `externalId` de transaction sont réservés aux `op_id` : aucune autre source ne doit en écrire.
- La relecture par `externalId` n'existe pas pour toutes les commandes : à vérifier pour chaque nouvelle commande utilisée (retrait forcé, transferts, écritures manuelles, dont `referenceNumber` n'est pas un filtre de lecture).
- Le cas « clé bloquée en cours » reste une procédure d'exploitation tant que Fineract ne nettoie pas ces lignes.
