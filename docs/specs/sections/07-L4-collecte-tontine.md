<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 424 à 476. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L4 — Collecte journalière, agent collecteur, tontine

#### L4a — Collecte journalière (`custom/nkulkap/collecte`)

**Ce que Fineract fournit déjà** : feuilles de collecte, guichets et caisses (Teller, Cashier), épargne programmée de fréquence journalière, épargne de groupe.

**Exigences**

- **FR-L4-01** — Rôle de collecteur : un membre du personnel Fineract lié à une agence, avec plafond d'encaisse, plafond hors-ligne par opération et par jour, et règle de commission.
- **FR-L4-02** — Carnet de collecte : lie un client à un compte d'épargne, un collecteur, une mise journalière (montant convenu), un cycle (31 jours par défaut) et une règle de frais (par défaut 1 mise par cycle **[À VÉRIFIER auprès des EMF pilotes]**).
- **FR-L4-03** — Tournée : ouverte par le collecteur en début de journée, elle regroupe toutes les collectes du jour. États : OUVERTE → SOUMISE → RAPPROCHÉE, ou EN_ÉCART.
- **FR-L4-04** — Chaque collecte est un dépôt Fineract sur le compte du carnet, réalisé par `nkulkap-sync` avec `Idempotency-Key = op_id`.
- **FR-L4-05** — Clôture de tournée : le collecteur remet les espèces au caissier, qui saisit le montant compté. L'écart est calculé contre le total des collectes comptabilisées. Un écart non nul génère une écriture sur le compte « écarts de caisse » et une alerte vers `nkulkap-sentinel`.
- **FR-L4-06** — Fin de cycle : une tâche planifiée prélève les frais de collecte et appelle `nkulkap-rules` (règle `collecte/frais_collecte`) pour répartir entre commission du collecteur et produit de l'EMF.
- **FR-L4-07** — Anti-fraude : chaque collecte affiche ou imprime un reçu avec un code court (6 caractères dérivés de `op_id`). Le client reçoit un SMS dès la comptabilisation. La position GPS est enregistrée si elle est disponible.
- **FR-L4-08** — Les rapports de collecte (par collecteur, par jour, par carnet, retards de mise) sont disponibles dans `nkulkap-web` et en rapports Fineract.

**Données** :

```sql
nkulkap_collecteur(id, staff_id UNIQUE, office_id, statut, plafond_encaisse BIGINT,
                plafond_hors_ligne_op BIGINT, plafond_hors_ligne_jour BIGINT,
                regle_commission VARCHAR, part_commission_pct NUMERIC(5,2))
nkulkap_carnet(id, client_id, savings_account_id UNIQUE, collecteur_id, mise_journaliere BIGINT,
            cycle_jours INT DEFAULT 31, regle_frais VARCHAR, debut DATE, statut)
nkulkap_tournee(id, collecteur_id, date_tournee, statut, total_comptabilise BIGINT,
             total_declare BIGINT, ecart BIGINT, rapprochee_par, rapprochee_le,
             UNIQUE(collecteur_id, date_tournee))
nkulkap_collecte_op(op_id UUID PK, carnet_id, tournee_id, montant BIGINT,
                 horodatage_appareil TIMESTAMPTZ, hlc VARCHAR, horodatage_serveur TIMESTAMPTZ,
                 savings_txn_id BIGINT, statut, lat NUMERIC(9,6), lon NUMERIC(9,6), code_recu CHAR(6))
```

**API** : `CRUD /nkulkap/collecteurs`, `CRUD /nkulkap/carnets`, `GET /nkulkap/tournees?collecteur&date`, `POST /nkulkap/tournees/{id}/soumettre`, `POST /nkulkap/tournees/{id}/rapprocher {montant_compte}`.

#### L4b — Tontine (`custom/nkulkap/tontine`)

La tontine rotative (njangi) est absente de Fineract.

> **[À VÉRIFIER]** Le cadre réglementaire applicable à une EMF qui gère des tontines pour ses clients doit être confirmé avec la COBAC ou un juriste avant la mise en production.

**Exigences**

- **FR-L4-10** — Une tontine est rattachée à un groupe Fineract. Elle définit : cotisation, fréquence (hebdomadaire, mensuelle), date de début, nombre de cycles (égal au nombre de membres), ordre de passage (FIXE ou TIRAGE en v1 ; ENCHÈRES en v2), pénalité de retard, frais de gestion de l'EMF.
- **FR-L4-11** — Chaque membre cotise depuis son compte d'épargne, en espèces par le collecteur, ou par mobile money. La cotisation crédite un compte de cagnotte (passif) de la tontine.
- **FR-L4-12** — À l'échéance d'un cycle, si la cagnotte est complète, elle est versée au bénéficiaire du cycle, moins les frais répartis par `nkulkap-rules`. Si elle est incomplète, le versement attend une validation manuelle et les retardataires sont pénalisés.
- **FR-L4-13** — Un bénéficiaire déjà servi qui cesse de cotiser est signalé en arriéré. Il est suivi comme un débiteur, avec relance par SMS.
- **FR-L4-14** — L'ordre de passage par TIRAGE est tracé : graine aléatoire enregistrée, résultat reproductible et auditable.

**Données** : `nkulkap_tontine`, `nkulkap_tontine_membre(position, a_recu)`, `nkulkap_tontine_cycle(numero, echeance, beneficiaire, statut, total_collecte)`, `nkulkap_tontine_cotisation(cycle_id, membre_id, montant, op_id, statut)`.

**Acceptation (L4)** : le scénario E2E-01 passe (300 collectes hors-ligne, 0 doublon, écart 0, commission juste) ; le scénario E2E-07 passe (tontine de 10 membres, un retard pénalisé).

