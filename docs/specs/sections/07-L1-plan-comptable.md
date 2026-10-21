<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 361 à 381. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L1 — Plan comptable CEMAC et hors-bilan (`custom/nkulkap/coa`)

**Ce que Fineract fournit déjà** : comptes hiérarchiques (HEADER/DETAIL), 5 types de comptes, étiquettes, règles comptables, mapping produit → comptes, écritures manuelles, clôtures, report à nouveau, provisionnement, balance.

**Exigences**

- **FR-L1-01** — Importer le plan comptable PCEMF à partir d'un fichier CSV versionné (`docs/regulatory-inputs/pcemf.csv`). Colonnes : `code`, `libelle_fr`, `libelle_en`, `classe`, `type` (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE), `usage` (HEADER ou DETAIL), `code_parent`, `hors_bilan` (booléen), `saisie_manuelle` (booléen).
- **FR-L1-02** — L'import est idempotent. Le relancer ne crée aucun doublon et met à jour les libellés. Il refuse de changer le type d'un compte qui a des écritures.
- **FR-L1-03** — Les comptes de classe 9 (engagements hors-bilan) sont créés par paires qui se compensent et portent l'étiquette `HORS_BILAN` (ADR-003). Une écriture hors-bilan débite toujours un compte de la paire et crédite l'autre.
- **FR-L1-04** — Fournir des gabarits de mapping produit → comptes pour : épargne à vue, dépôt à terme, épargne programmée de collecte, crédit individuel, crédit de groupe, tontine.
- **FR-L1-05** — Configurer les « financial activity mappings » de Fineract : caisse, coffre, fonds en transit mobile money, écarts de caisse, opérations en suspens.
- **FR-L1-06** — Contrôle de cohérence : un rapport vérifie que tout compte DETAIL a un parent, que la somme des débits égale celle des crédits, et que les comptes `HORS_BILAN` sont absents du bilan.

**Données** : `nkulkap_coa_import(id, version, fichier_sha256, importe_le, importe_par, lignes_creees, lignes_maj)`.

**API** :
- `POST /nkulkap/coa/import` (multipart CSV, paramètre `dryRun=true|false`) → rapport de différences.
- `GET /nkulkap/coa/check` → rapport de cohérence.

**Acceptation** : 100 % des lignes du fichier PCEMF importées ; un second import produit 0 création ; la balance par classe est équilibrée ; le bilan exclut la classe 9.

