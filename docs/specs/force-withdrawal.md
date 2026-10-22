# Retrait forcé (`command=force-withdrawal`)

Étape 1.8. Comportement observé le 2026-10-22 sur la pile locale (Fineract `b94b2ddf`) et figé par `testing/spikes/src/test/java/cm/nkulkap/spikes/ForceWithdrawalSpikeIT.java` (`make spike-test SPIKE=ForceWithdrawalSpikeIT`, pile démarrée et `make seed` passé).

## Requête

`POST /savingsaccounts/{id}/transactions?command=force-withdrawal`, même corps qu'un retrait :

```json
{"transactionDate": "2026-02-01", "transactionAmount": 60000, "paymentTypeId": 4,
 "dateFormat": "yyyy-MM-dd", "locale": "fr"}
```

Réponse en cas de succès : `200` `{"officeId", "clientId", "savingsId", "resourceId", "changes": {"paymentTypeId": 4}}`. La transaction créée est de type **`Withdrawal`** : rien ne la distingue ensuite d'un retrait ordinaire dans `GET /savingsaccounts/{id}/transactions/{txId}`.

## Configuration requise

| Élément | Valeur |
|---|---|
| Interrupteur | configuration globale `allow-force-withdrawal-on-savings-account` (`GET /configurations/name/allow-force-withdrawal-on-savings-account`, ici `id` 64), désactivée par défaut |
| Plafond | champ `value` de la même configuration, en unités de la devise : le solde après retrait doit rester ≥ −plafond. `value` 0 : aucun découvert. |
| Activation | `PUT /configurations/{id}` `{"enabled": true, "value": 50000}` → `200 {"resourceId": 64, "changes": {...}}` |
| Portée | **tout le locataire** : aucun réglage par produit ni par compte (vérifié dans le code : pas d'attribut `allowForce*`). |

Code : interrupteur lu par `ConfigurationDomainServiceJpa` ; contrôle dans `SavingsAccount.isForceWithdrawalAllowed` (`fineract-savings`), appliqué seulement quand le retrait ferait passer sous le solde minimal.

## Comportement

| Situation | Résultat |
|---|---|
| Désactivé, montant > solde | `403` `error.msg.savingsaccount.transaction.insufficient.account.balance` (même code qu'un retrait ordinaire) |
| Désactivé, montant ≤ solde | `200` : agit comme un retrait ordinaire |
| Activé (plafond 50 000), solde 40 000, retrait forcé 60 000 | `200`, solde −20 000 |
| Puis retrait forcé 40 000 (solde −60 000) | `403` même code « solde insuffisant » : aucun code propre au dépassement de plafond |
| Puis retrait **ordinaire** de 100 à découvert | `403` : seul le retrait forcé peut créer ou creuser le découvert |
| Puis retrait forcé 30 000 (solde −50 000 = −plafond) | `200` : la borne est incluse |

## Permission

Code `FORCE_WITHDRAWAL_SAVINGSACCOUNT`, distinct de `WITHDRAWAL_SAVINGSACCOUNT` (variante maker-checker `FORCE_WITHDRAWAL_SAVINGSACCOUNT_CHECKER`). Un utilisateur qui n'a que `WITHDRAWAL_SAVINGSACCOUNT` et `READ_SAVINGSACCOUNT` :
- retrait forcé → `403` `error.msg.not.authorized`, `"developerMessage": "User has no authority to: FORCE_WITHDRAWAL_SAVINGSACCOUNT"` ;
- retrait ordinaire → `200`.

## Écritures produites (produit en comptabilité de caisse)

Retrait forcé de 60 000 sur un solde de 40 000 (transaction `S<id>`), comptes du seed FICTIF :

| Compte | Mapping du produit | Sens | Montant |
|---|---|---|---|
| `FICTIF-3711` Dépôts à vue de la clientèle | `savingsControlAccountId` | Débit | 40 000 |
| `FICTIF-2031` Découverts à la clientèle | `overdraftPortfolioControlId` | Débit | 20 000 |
| `FICTIF-5711` Caisse agence | `savingsReferenceAccountId` | Crédit | 60 000 |

La part qui dépasse le solde est donc constatée comme une créance sur le client (actif), et non comme un dépôt négatif. Dans la limite du solde, l'écriture est celle d'un retrait ordinaire (débit dépôts, crédit caisse).

## Conséquences pour NKULKAP

- L'interrupteur étant global, l'activer ouvre le découvert à **tous** les comptes d'épargne du locataire, pour tout utilisateur doté de la permission. Si le cahier réserve le retrait forcé à certains produits (collecte, tontine), il faudra une garde NKULKAP : écouteur avant transaction (mécanisme de l'étape 1.5) ou contrôle dans le satellite. À trancher en phase 6.
- Le plafond est un montant unique pour tout le locataire, pas par client.
- Le refus pour dépassement ne se distingue pas d'un solde insuffisant : un satellite ne peut pas afficher un message plus précis sans relire la configuration.
- Le compte `overdraftPortfolioControlId` du produit doit être un vrai compte de créances (plan comptable, phase 3).
