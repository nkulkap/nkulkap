<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 735 à 761. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 11. Plan d'implémentation pas à pas

### 11.1 Vue d'ensemble

| Phase | Contenu | Taille | Dépend de |
|---|---|---|---|
| 0 | Préparer : dépôts, outillage, pile de développement | M | — |
| 1 | Comprendre : explorations et vérifications d'hypothèses | M | 0 |
| 2 | Socle commun : contrats, données de test, réseau dégradé, identité | M | 1 |
| 3 | L1 Plan comptable et hors-bilan | M | 2 |
| 4 | L2 TEG et usure | M | 3 |
| 5 | L3 Numscript | S | 3 |
| 6 | L4 Collecte et tontine | L | 3, 5 |
| 7 | L5 Hors-ligne (sync, app, nœud d'agence) | XL | 6 |
| 8 | L6 Mobile money et SMS | L | 5, 7 |
| 9 | L7 Reporting réglementaire | M | 3 |
| 10 | L8 LAB-FT | L | 7, 8 |
| 11 | L9 Interbancaire (adaptateur par fichiers) | S | 8 |
| 12 | Bout en bout et industrialisation | L | Tout |
| 13 | Migration des données et pilote | L | 12 |

Tailles : S ≈ 1 à 2 semaines, M ≈ 2 à 4, L ≈ 4 à 8, XL ≈ 8 à 12, pour une équipe de 4 à 6 ingénieurs assistés par Claude Code. **Estimation indicative**, à recalibrer après la phase 1.

Les phases 4, 5 et 9 peuvent avancer en parallèle de la phase 6.

---

