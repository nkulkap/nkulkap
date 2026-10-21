<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 382 à 405. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L2 — TEG et plafond réglementaire (`custom/nkulkap/teg`)

**Base réglementaire** : Règlement n° 04/19/CEMAC/UMAC/CM relatif au TEG, à la répression de l'usure et à la publication des conditions de banque. Les taux d'usure sont fixés par le Comité de Politique Monétaire de la BEAC, par catégorie de crédit, et révisés trimestriellement **[À VÉRIFIER : texte officiel, liste des catégories, coûts inclus dans le TEG, application aux EMF]**.

**Exigences**

- **FR-L2-01** — Calculer le TEG annuel de tout prêt par la méthode actuarielle (annexe E). Les flux comprennent : capital décaissé, intérêts, frais prélevés au décaissement, frais périodiques, assurance obligatoire, et toute charge Fineract marquée « incluse au TEG ».
- **FR-L2-02** — Chaque charge Fineract porte un attribut NKULKAP `inclus_teg` (vrai par défaut pour les frais liés à l'octroi). Il est stocké dans `nkulkap_charge_teg(charge_id, inclus_teg)`.
- **FR-L2-03** — Tenir une table des taux d'usure par catégorie et par période de validité, saisie par un administrateur avec la référence de la décision.
- **FR-L2-04** — Associer chaque produit de prêt à une catégorie réglementaire.
- **FR-L2-05** — Bloquer tout décaissement dont le TEG dépasse le taux d'usure applicable à la date d'octroi. Le blocage se fait par un écouteur sur l'événement pré-métier `LoanDisbursalBusinessEvent`, enregistré avec `addPreBusinessEventListener`. Aucune dérogation n'est possible.
- **FR-L2-06** — Simulation avant contrat : un endpoint calcule TEG et échéancier à partir des paramètres d'un produit, sans créer de prêt.
- **FR-L2-07** — Générer la fiche TEG remise au client avant signature (PDF, FR ou EN selon la langue du client) : montant, durée, taux nominal, TEG, coût total, échéancier.
- **FR-L2-08** — Rapport trimestriel des TEG moyens par catégorie, pour la déclaration au CNEF **[À VÉRIFIER : format]**.

**Données** :
- `nkulkap_usury_rate(id, categorie, taux_annuel NUMERIC(9,6), valide_du, valide_au, reference_decision, saisi_par)`
- `nkulkap_product_category(loan_product_id PK, categorie)`
- `nkulkap_loan_teg(loan_id PK, teg NUMERIC(9,6), calcule_le, taux_usure_applique, conforme BOOL)`

**API** : `GET /nkulkap/loans/{id}/teg`, `POST /nkulkap/teg/simulate`, `GET /nkulkap/loans/{id}/teg/fiche.pdf`, `CRUD /nkulkap/usury-rates`.

**Acceptation** : TEG exact au 1/10 000e de point près sur 20 cas de référence calculés à la main ou dans un tableur (fichier `testing/fixtures/teg_golden.csv`) ; décaissement bloqué au-delà de l'usure, avec message FR/EN.

