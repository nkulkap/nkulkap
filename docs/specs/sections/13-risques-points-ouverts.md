<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1221 à 1243. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 13. Risques et points ouverts

| Risque | Impact | Probabilité | Parade |
|---|---|---|---|
| Comportement d'idempotence de Fineract différent de l'hypothèse | Doublons | Moyenne | Vérifié dès l'étape 1.2 ; déduplication propre à `nkulkap-sync` en repli |
| Modules custom Fineract encore en « proof of concept » | Casse lors des mises à jour amont | Moyenne | Fonctions ajoutées plutôt que remplacées ; tests d'intégration contre chaque nouvelle version ; contributions amont |
| Formats réglementaires indisponibles | Reporting non conforme | Haute | Moteur piloté par gabarits ; démarches COBAC lancées dès le début |
| Accès opérateurs longs à obtenir | Retard sur L6 | Haute | Simulateurs fidèles ; démarches commerciales lancées dès la phase 0 |
| Retraits hors-ligne frauduleux ou en double | Pertes | Moyenne | Plafonds bas par défaut ; revue superviseur ; règles sentinel |
| Rareté des profils Rust et Scala au Cameroun | Composants orphelins | Haute | Composants petits et bornés ; documentation ; tests exhaustifs ; formation interne ; Claude Code en appui |
| Obligations MPL mal appliquées | Risque juridique | Faible | Code nouveau en fichiers nouveaux ; publication des fichiers MPL modifiés ; revue juridique |
| Dérive des versions amont (Fineract, web-app, android-client) | Dette | Haute | Renovate ; resynchronisation trimestrielle planifiée |
| Coût de l'hébergement local | Marge | Moyenne | Mesures de charge (12.3) ; mutualisation SaaS |

**Pistes v2** :
- libre-service client par USSD ;
- transport des opérations par SMS quand la donnée mobile est indisponible mais que le réseau GSM répond ;
- tontine aux enchères ;
- application client ;
- connecteurs temps réel GIMACPAY.

---

