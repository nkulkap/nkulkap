<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 938 à 957. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 5 — L3 Numscript

**Étape 5.1 — Service `nkulkap-rules`**
- **Faire** : service Go avec `POST /v1/evaluate` (FR-L3-01), `GET /v1/rules` (liste, versions, hachages) et `GET /healthz`. Chargement des règles au démarrage depuis `rules/`. Refus de démarrer si un script ne s'analyse pas. Aucun état. Image Docker distroless.
- **Terminé quand** : tests HTTP et tests de charge simples (≥ 2 000 évaluations/s sur un cœur).

**Étape 5.2 — Règles v1 et tests**
- **Faire** : `rules/collecte/frais_collecte.num`, `rules/momo/frais_retrait.num`, `rules/tontine/frais_gestion.num`, chacune avec un fichier de cas `*.cases.json` (entrées → écritures attendues) exécuté par `go test`.
- **Terminé quand** : FR-L3-02, FR-L3-05 et FR-L3-06 vérifiés ; somme des écritures = montant d'entrée sur 10 000 tirages aléatoires.

**Étape 5.3 — Correspondance des comptes et écriture Fineract**
- **Faire** : `rules/account_map.yaml` ; dans `custom/nkulkap/common`, transformation des écritures Numscript en une écriture comptable Fineract (FR-L3-03, FR-L3-04).
- **Terminé quand** : test d'intégration où un appel de répartition produit une écriture équilibrée dans Fineract, avec `referenceNumber = op_id`, et où un second appel ne crée rien.

**Étape 5.4 — Grille de frais**
- **Faire** : table `nkulkap_fee_grid(code, tranche_min, tranche_max, frais_fixe, frais_pct)`, avec son API et un calcul pur testé (FR-L3-07).
- **Terminé quand** : bornes de tranches testées (min inclus, max exclu, documenté).

---

