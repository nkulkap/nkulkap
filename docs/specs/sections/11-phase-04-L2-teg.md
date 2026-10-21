<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 905 à 937. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 4 — L2 TEG et usure

**Étape 4.1 — Calculateur pur**
- **Faire** : `TegCalculator` (annexe E) en Java pur, sans dépendance à Fineract. Entrée : liste de flux datés ; sortie : TEG annuel en `BigDecimal` (scale 6). Newton-Raphson avec repli par dichotomie, convergence à 1e-9, au plus 200 itérations.
- **Tests** : `testing/fixtures/teg_golden.csv` (20 cas, dont les 2 de l'annexe E), propriétés jqwik (TEG croissant avec les frais ; TEG = taux nominal actuariel quand il n'y a pas de frais, aux conventions de jours près).
- **Terminé quand** : 100 % des cas de référence passent à ±0,0001 point.

**Étape 4.2 — Tables réglementaires**
- **Faire** : migrations `nkulkap_usury_rate`, `nkulkap_product_category` et `nkulkap_charge_teg`, avec leurs API CRUD (FR-L2-02 à 04). Contrôle anti-chevauchement des périodes de validité.
- **Terminé quand** : tests de chevauchement et de recherche du taux applicable à une date donnée.

**Étape 4.3 — Extraction des flux d'un prêt**
- **Faire** : convertir l'échéancier Fineract et les charges du prêt en flux datés, selon les règles d'inclusion.
- **Terminé quand** : 5 prêts de démonstration donnent le même TEG que le calcul de référence sur tableur.

**Étape 4.4 — Veto**
- **Faire** : écouteur avant décaissement (validé à l'étape 1.5) qui calcule le TEG, le stocke dans `nkulkap_loan_teg` et bloque en cas de dépassement (FR-L2-05).
- **Terminé quand** : E2E-06, partie blocage.

**Étape 4.5 — Simulation et fiche TEG**
- **Faire** : `POST /nkulkap/teg/simulate` ; génération PDF (PDFBox) FR et EN de la fiche.
- **Terminé quand** : PDF généré pour un prêt de démonstration dans les deux langues, et relu.

**Étape 4.6 — Rapport trimestriel**
- **Faire** : TEG moyens par catégorie et par trimestre (FR-L2-08), au format CSV en attendant le format officiel.
- **Terminé quand** : rapport produit sur le jeu de démonstration.

**Étape 4.7 — Écrans**
- **Faire** : gestion des taux d'usure, affichage du TEG sur la fiche du prêt, simulateur.
- **Terminé quand** : parcours validés en FR et en EN.

---

