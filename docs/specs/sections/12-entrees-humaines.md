<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1198 à 1220. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 12. Entrées humaines requises

Claude Code ne peut pas les produire. En attendant, il utilise les remplacements indiqués, marqués `FICTIF`.

| ID | Entrée | Requise pour | Où la trouver | Remplacement en attendant |
|---|---|---|---|---|
| I-01 | PCEMF officiel en vigueur (liste complète des comptes) | Phase 3 | BEAC / COBAC (le document de janvier 2010 est publié sur beac.int ; vérifier s'il existe une version plus récente) | `pcemf.FICTIF.csv` |
| I-02 | Formats de télédéclaration SPECTRA / SESAME 4.0 | Phase 9 | COBAC, ou une EMF agréée | Sorties XLSX/CSV génériques |
| I-03 | Définitions des ratios prudentiels EMF | Phase 9 | Règlement COBAC relatif aux EMF | Aucun gabarit de ratio |
| I-04 | Texte du Règlement 04/19/CEMAC/UMAC/CM ; taux d'usure en vigueur ; catégories ; coûts inclus dans le TEG ; convention de décompte des jours | Phase 4 | BEAC, CNEF, Journal officiel | Taux de test, convention réel/365 |
| I-05 | Règlement CEMAC LAB-FT en vigueur, seuils, format de déclaration de soupçon ANIF | Phase 10 | ANIF, GABAC | Seuils de test, PDF générique |
| I-06 | Règles fiscales sur frais et intérêts (TVA 19,25 %, retenues sur intérêts d'épargne) | Phases 5, 8 | Expert fiscal | Taux de test documentés |
| I-07 | Statut réglementaire des tontines gérées par une EMF | Phase 6 | COBAC, juriste | Développement poursuivi, mise en production bloquée |
| I-08 | Accès au bac à sable puis à la production MTN MoMo Cameroun (contrat marchand) | Phase 8 | MTN Cameroon | Simulateur |
| I-09 | Accès à l'API marchande Orange Money Cameroun | Phase 8 | Orange Cameroun | Simulateur |
| I-10 | Contrat avec un agrégateur SMS | Phase 8 | Agrégateur local | Fournisseur « journal » (écrit dans les logs) |
| I-11 | Spécifications GIMAC / GIMACPAY, SYSTAC, SYGMA | Phase 11 | GIMAC, BEAC (via une banque partenaire) | Adaptateur par fichiers |
| I-12 | Texte de la loi n° 2024/017 et position de l'autorité sur l'hébergement | Phase 12 | Journal officiel, juriste | Hébergement au Cameroun |
| I-13 | Pratiques des EMF pilotes : frais de collecte, cycle, heure limite de date de valeur, plafonds hors-ligne | Phases 6, 7 | Entretiens avec 5 à 10 EMF | Valeurs par défaut de ce document |
| I-14 | Données de l'ancien système de l'EMF pilote | Phase 13 | EMF pilote, sous accord de confidentialité | Jeu fictif |

---

