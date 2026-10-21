<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1519 à 1541. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Annexe E — Calcul du TEG

Méthode actuarielle. Le TEG annuel `r` est le taux qui annule la valeur actuelle des flux du prêt :

```latex
\sum_{k=0}^{n} \frac{F_k}{(1+r)^{t_k}} = 0
```

- `F_0` = montant net reçu par l'emprunteur (capital − frais prélevés au décaissement), positif ;
- `F_k` = chaque paiement de l'emprunteur (échéance + frais périodiques + assurance obligatoire), négatif ;
- `t_k` = durée écoulée en années depuis le décaissement, convention réel/365 **[À VÉRIFIER, I-04]**.

Résolution : Newton-Raphson à partir de `r = taux nominal`, repli par dichotomie sur [−0,99 ; 10] si la dérivée s'annule ou si l'itération sort de l'intervalle.

**Cas de référence** (calculés pour ce document, à intégrer dans `teg_golden.csv`) :

| Cas | Données | Échéancier | TEG attendu |
|---|---|---|---|
| E-1 | 100 000 XAF, 12 mensualités, taux nominal 24 %/an (2 %/mois), décaissé le 15/01/2026, sans frais | 11 × 9 456 + 1 × 9 454 | **26,9497 %** |
| E-2 | Comme E-1 + 2 000 XAF de frais de dossier au décaissement + 300 XAF d'assurance par mois | Comme E-1 + 300 par mois | **40,2688 %** |

> E-1 montre l'effet de la convention de jours : avec des mois égaux (1/12 d'année), le résultat serait (1,02)^12 − 1 = 26,8242 %. La convention retenue doit être celle du règlement.

