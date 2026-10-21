<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1076 à 1107. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 9 — L7 Reporting réglementaire

**Étape 9.1 — Grammaire des formules**
- **Faire** : analyseur et évaluateur purs (FR-L7-02), avec messages d'erreur positionnés.
- **Terminé quand** : 50 tests d'analyse, dont les erreurs ; propriété : `SOLDE(A*) = SOLDE_DEBITEUR(A*) - SOLDE_CREDITEUR(A*)`.

**Étape 9.2 — Extraction des soldes**
- **Faire** : soldes par compte à une date, par agence et par étiquette, à partir des tables comptables Fineract (lecture SQL optimisée, index vérifiés).
- **Terminé quand** : soldes identiques à la balance Fineract (« Trial Balance ») sur le jeu de démonstration.

**Étape 9.3 — Gabarits et rendus**
- **Faire** : chargeur YAML, rendus XLSX (Apache POI, mise en forme du gabarit) et CSV ; point d'extension `sesame`.
- **Terminé quand** : un gabarit d'exemple produit un XLSX relu manuellement.

**Étape 9.4 — Productions figées**
- **Faire** : FR-L7-03, 04 et 07 ; API `POST /nkulkap/reports/{code}/productions`, `GET .../{id}` et téléchargement.
- **Terminé quand** : deux productions à la même date donnent le même hachage ; une production est refusée sur période non clôturée.

**Étape 9.5 — Gabarits v1**
- **Faire** : bilan, compte de résultat, balance par classe, PAR, ancienneté (FR-L7-05). Les ratios prudentiels sont livrés dès que les définitions sont obtenues (I-03).
- **Terminé quand** : E2E-08 passe.

**Étape 9.6 — Centrale des risques**
- **Faire** : gabarit paramétrable (FR-L7-06).
- **Terminé quand** : fichier produit sur le jeu de démonstration.

**Étape 9.7 — Écrans**
- **Faire** : liste des gabarits, production, historique, téléchargement.
- **Terminé quand** : parcours validés.

---

