<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 514 à 544. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L6 — MTN MoMo et Orange Money (`nkulkap-pay`)

**Sources consultées** : SDK Java MoMo de MTN, guides d'intégration Orange Money Web Payment au Cameroun. Les détails d'API (en-têtes, statuts, délais) **[À VÉRIFIER]** sur la documentation officielle obtenue avec les accès marchand.

**Exigences**

- **FR-L6-01** — Opérations prises en charge :
  - `CASH_IN` : wallet → compte d'épargne ;
  - `REPAYMENT` : wallet → prêt ;
  - `TONTINE` : wallet → cotisation ;
  - `CASH_OUT` : compte d'épargne → wallet ;
  - `DISBURSEMENT` : décaissement de prêt → wallet.
- **FR-L6-02** — Encaissement par « demande de paiement » (USSD push), validée par le client sur son propre téléphone, y compris un téléphone basique. MTN : produit *Collection*, `requestToPay`. Orange : paiement marchand.
- **FR-L6-03** — Machine à états (annexe D.1) : `CREATED → SUBMITTED → PENDING → SUCCEEDED → POSTED`, avec les branches `FAILED`, `EXPIRED`, `UNKNOWN` et `REVERSED`.
- **FR-L6-04** — Idempotence : l'identifiant de l'intention de paiement (UUID) est l'identifiant de requête côté opérateur (MTN : `X-Reference-Id`) et la clé d'idempotence côté Fineract.
- **FR-L6-05** — Rappels de l'opérateur : authentifiés (secret dans l'URL et liste d'IP autorisées), dédoublonnés, et **toujours confirmés** par une requête de statut avant d'agir.
- **FR-L6-06** — Interrogation : toute intention en `PENDING` est réinterrogée selon un calendrier (1, 2, 5, 10, 30 minutes, puis toutes les heures). Après 24 heures, elle passe en `UNKNOWN` et rejoint la file de rapprochement.
- **FR-L6-07** — `CASH_OUT` et `DISBURSEMENT` : on débite d'abord Fineract vers le compte « fonds en transit mobile money », puis on appelle l'opérateur. En cas d'échec définitif, on contre-passe l'écriture.
- **FR-L6-08** — Rapprochement J+1 : import des relevés opérateur (API ou CSV), appariement par référence opérateur, puis par montant, MSISDN et date. Le rapport d'écarts est classé en 4 catégories : manquant chez nous, manquant chez l'opérateur, montant différent, en double.
- **FR-L6-09** — Numéros : normalisation au format E.164 `+2376XXXXXXXX`. La table `nkulkap_msisdn_prefix` associe chaque préfixe à un opérateur et se configure sans code.
- **FR-L6-10** — Frais : la grille `nkulkap_fee_grid` donne le montant des frais, puis la règle `momo/frais_retrait` les répartit (annexe B.2).
- **FR-L6-11** — Disjoncteur par opérateur : en cas d'indisponibilité, les nouvelles demandes sont refusées avec un message clair, et non mises en attente indéfiniment.

**Module `notify` (SMS)**
- **FR-L6-20** — Interface de fournisseur SMS indépendante de l'agrégateur ; le fournisseur se choisit par configuration.
- **FR-L6-21** — Gabarits FR et EN en GSM-7 : les accents sont translittérés pour tenir dans 160 caractères, car un seul accent fait passer le SMS en UCS-2 et sa limite tombe à 70 caractères.
- **FR-L6-22** — Déclencheurs : `op.posted` (collecte, dépôt, remboursement), résultat de paiement mobile money, rappel d'échéance la veille, arriéré de tontine.
- **FR-L6-23** — Envoi idempotent (`op_id` + gabarit), avec nouvelles tentatives et suivi des accusés de réception si l'agrégateur les fournit.

**Acceptation** : E2E-04 et E2E-05 contre les simulateurs, plus une série de tests sur l'environnement sandbox MTN (devise de test à vérifier).

