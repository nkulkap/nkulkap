<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 877 à 904. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 3 — L1 Plan comptable et hors-bilan

**Étape 3.1 — Fichier PCEMF**
- **Faire** : si le PCEMF officiel est obtenu (section 12, entrée I-01), le transcrire en `docs/regulatory-inputs/pcemf.csv`. Sinon, créer `pcemf.FICTIF.csv` (environ 80 comptes, toutes classes, dont la classe 9) et le noter dans `STATUS.md`. Écrire l'analyseur et le validateur : codes uniques, parents existants, types cohérents par classe.
- **Terminé quand** : le validateur rejette 10 fichiers volontairement invalides avec un message précis.

**Étape 3.2 — Import idempotent**
- **Faire** : service d'import qui appelle les services Fineract internes (même JVM) de création et de mise à jour de comptes GL. Il crée la hiérarchie dans l'ordre parent → enfant, avec un mode « simulation » qui renvoie le différentiel. Endpoint `POST /nkulkap/coa/import`.
- **Terminé quand** : FR-L1-01 et FR-L1-02 vérifiés par tests d'intégration.

**Étape 3.3 — Hors-bilan**
- **Faire** : création des valeurs de code d'étiquette `HORS_BILAN`, appariement des comptes de classe 9 et validation qu'une écriture hors-bilan reste dans sa paire.
- **Terminé quand** : FR-L1-03 vérifié.

**Étape 3.4 — Gabarits de mapping produit**
- **Faire** : fichiers JSON de gabarits (FR-L1-04), endpoint pour appliquer un gabarit à un produit, et configuration des « financial activity mappings » (FR-L1-05).
- **Terminé quand** : un produit d'épargne créé avec le gabarit génère les bonnes écritures sur dépôt et retrait (test).

**Étape 3.5 — Contrôle de cohérence**
- **Faire** : `GET /nkulkap/coa/check` (FR-L1-06).
- **Terminé quand** : le contrôle détecte 5 anomalies injectées.

**Étape 3.6 — Écrans `nkulkap-web`**
- **Faire** : écran d'import (fichier, simulation, différentiel, confirmation) et écran de contrôle. Libellés FR et EN.
- **Terminé quand** : parcours manuel validé et test end-to-end de l'interface (Playwright ou Cypress selon l'outil déjà présent dans `web-app`).

---

