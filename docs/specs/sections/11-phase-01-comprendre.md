<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 794 à 841. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 1 — Comprendre

Chaque exploration produit **un test automatisé** qui fige le comportement observé et **une note** dans `docs/specs/`. Si une hypothèse du présent document est fausse, s'arrêter, rédiger un ADR correctif et mettre à jour ce document.

**Étape 1.1 — Tour de Fineract**
- **Faire** : script `testing/fixtures/seed` qui crée par API, avec des données FICTIVES : 2 agences, 3 employés, 1 produit d'épargne, 1 produit de prêt, 10 clients, des comptes, des dépôts et un prêt décaissé. Documenter dans `docs/specs/fineract-api-usage.md` chaque endpoint utilisé par NKULKAP (chemin, corps, réponse, erreurs).
- **Terminé quand** : `make seed` est rejouable et la note couvre au minimum les endpoints de `glaccounts`, `journalentries`, `savingsaccounts/{id}/transactions` (commandes `deposit`, `withdrawal`, `force-withdrawal`), `loans/{id}/transactions` (commande `repayment`), `runreports` et `externalevents/configuration`.

**Étape 1.2 — Vérifier l'idempotence de Fineract (critique)**
- **Faire** : test d'intégration `IdempotencySpikeIT` couvrant :
  - (a) même dépôt d'épargne envoyé 2 fois avec la même clé `Idempotency-Key` ;
  - (b) même remboursement de prêt envoyé 2 fois ;
  - (c) 2 envois simultanés avec la même clé ;
  - (d) même clé, corps différent ;
  - (e) envoi rejoué après un redémarrage de Fineract.
- **Terminé quand** : le test passe ; la note `docs/specs/idempotence-fineract.md` décrit le comportement exact pour chaque cas (code HTTP, en-têtes, corps) ; **une seule transaction existe dans tous les cas**.
- **Si un cas produit un doublon** : `nkulkap-sync` doit ajouter sa propre table de déduplication avant l'appel, avec vérification par `externalId` ou par `referenceNumber`. Rédiger un ADR.

**Étape 1.3 — Événements externes Kafka**
- **Faire** : activer les événements nécessaires (clients, transactions d'épargne et de prêt ; Fineract ne publie pas d'événement d'écriture comptable, voir ADR-017) ; consommateur jetable qui décode l'Avro avec les schémas de `fineract-avro-schemas`. Lister les types d'événements, leurs champs utiles et leur latence.
- **Terminé quand** : la note `docs/specs/fineract-events.md` existe et un test lit un événement de dépôt.

**Étape 1.4 — Module custom minimal**
- **Faire** : dans `custom/nkulkap/common` :
  - une migration `db/custom-changelog` qui crée `nkulkap_ping` ;
  - une ressource JAX-RS `GET /nkulkap/ping` ;
  - une auto-configuration (fichier `AutoConfiguration.imports`).
- **Vérifier** : `curl .../api/v1/nkulkap/ping` renvoie `{"status":"ok","module":"nkulkap-common"}`.
- **Terminé quand** : le module est chargé dans l'image `fineract-custom` et le test d'intégration passe.

**Étape 1.5 — Veto avant décaissement**
- **Faire** : écouteur enregistré par `BusinessEventNotifierService.addPreBusinessEventListener(LoanDisbursalBusinessEvent.class, ...)`, qui lève une exception métier pour un prêt marqué « test-veto ».
- **Terminé quand** : le décaissement est refusé, aucune écriture n'est créée (annulation complète) et le message d'erreur est remonté au client de l'API. Sinon, rechercher un autre point d'extension et rédiger un ADR.

**Étape 1.6 — Audit de l'app Android**
- **Faire** : compiler `apps/nkulkap-field` (fork d'`android-client`), le lancer sur émulateur contre la pile locale, et parcourir le mode hors-ligne. Lister dans `docs/specs/field-offline-audit.md` les écrans, entités Room, dépôts de données et appels réseau de chaque flux hors-ligne (`feature/offline`, `core/database`).
- **Terminé quand** : la note confirme ou infirme l'absence d'idempotence et liste les fichiers à modifier en phase 7.

**Étape 1.7 — Numscript**
- **Faire** : squelette `services/nkulkap-rules` qui exécute les deux scripts de l'annexe B et retrouve exactement les sorties attendues.
- **Terminé quand** : `go test ./...` passe.

**Étape 1.8 — Retrait forcé**
- **Faire** : tester `command=force-withdrawal` : configuration requise, permission, effet sur le solde, écriture produite.
- **Terminé quand** : la note `docs/specs/force-withdrawal.md` existe et un test d'intégration la fige.

---

