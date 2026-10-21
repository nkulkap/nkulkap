<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 545 à 562. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L7 — États COBAC et centrale des risques (`custom/nkulkap/reporting`)

**Contexte vérifié** : la COBAC collecte les états réglementaires des EMF par la plateforme SPECTRA, module SESAME 4.0 (télédéclaration). Le format exact des fichiers attendus n'est pas public **[À VÉRIFIER : à obtenir auprès de la COBAC ou d'une EMF agréée]**.

**Principe** : un moteur générique d'états piloté par gabarits, qui se code dès maintenant. Les gabarits officiels seront ajoutés dès que les formats seront obtenus.

**Exigences**

- **FR-L7-01** — Un gabarit d'état est un fichier YAML versionné dans `reports/templates/`. Il contient des métadonnées (code, version, périodicité, langue) et des lignes. Chaque ligne a un code, un libellé et une formule sur les soldes GL.
- **FR-L7-02** — Grammaire des formules : `SOLDE(651*)`, `DEBIT(...)`, `CREDIT(...)`, `SOLDE_DEBITEUR(...)`, `SOLDE_CREDITEUR(...)`, opérateurs `+ - * /`, références à d'autres lignes (`L12 + L13`), filtres par agence et par étiquette (`ETIQUETTE(HORS_BILAN)`).
- **FR-L7-03** — Production à une date d'arrêté, après clôture de la période. Sorties : XLSX (mise en page du gabarit), CSV, et un adaptateur `sesame` à brancher quand le format sera connu.
- **FR-L7-04** — Chaque production est figée et auditable : données sources, version du gabarit, hachage SHA-256 du fichier, auteur, date. On ne la régénère jamais en silence.
- **FR-L7-05** — Gabarits livrés en v1 : bilan, compte de résultat, balance par classe, portefeuille à risque (PAR 1, 30, 90), ancienneté des impayés, ainsi que les ratios prudentiels EMF dès que leurs définitions officielles sont obtenues.
- **FR-L7-06** — Centrale des risques : un gabarit de déclaration des encours par emprunteur au-delà d'un seuil, avec identifiants clients normalisés **[À VÉRIFIER : dispositif applicable aux EMF, format, seuil]**.
- **FR-L7-07** — Contrôles bloquants avant export : balance équilibrée, période clôturée, aucun compte DETAIL orphelin.

**Acceptation** : E2E-08 ; les états du jeu de données de démonstration se recalculent à l'identique, au franc près, sur deux exécutions.

