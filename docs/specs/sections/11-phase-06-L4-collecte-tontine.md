<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 958 à 985. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 6 — L4 Collecte et tontine

**Étape 6.1 — Collecteurs et carnets**
- **Faire** : migrations, entités, services et API (FR-L4-01, FR-L4-02). Un carnet crée ou lie un compte d'épargne du produit « épargne de collecte ».
- **Terminé quand** : CRUD testé ; contrôle d'unicité compte ↔ carnet.

**Étape 6.2 — Tournées**
- **Faire** : cycle de vie (FR-L4-03), enregistrement des collectes (`nkulkap_collecte_op`, appelé par `nkulkap-sync`), clôture et rapprochement (FR-L4-05), écriture d'écart, événement `tournee.ecart`.
- **Terminé quand** : tests des états et d'une tournée avec écart positif, négatif et nul.

**Étape 6.3 — Fin de cycle**
- **Faire** : tâche planifiée Fineract (modèle `custom/acme/loan/job`) qui prélève les frais de collecte et répartit la commission par `nkulkap-rules` (FR-L4-06).
- **Terminé quand** : sur 31 jours simulés, frais et commission justes au franc près.

**Étape 6.4 — Tontine : modèle et API**
- **Faire** : FR-L4-10 et FR-L4-11 ; tirage au sort reproductible (FR-L4-14).
- **Terminé quand** : tests de création, d'adhésion, de tirage et de cotisation.

**Étape 6.5 — Tontine : échéances**
- **Faire** : tâches d'échéance, versement, pénalités et arriérés (FR-L4-12, FR-L4-13).
- **Terminé quand** : E2E-07 en test d'intégration (sans l'app).

**Étape 6.6 — Rapports et écrans**
- **Faire** : écrans `nkulkap-web` (collecteurs, carnets, tournées, rapprochement, tontines) et rapports (FR-L4-08).
- **Terminé quand** : parcours validés en FR et en EN.

---

