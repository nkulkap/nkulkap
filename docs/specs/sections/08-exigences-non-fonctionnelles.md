<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 614 à 635. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 8. Exigences non fonctionnelles

| ID | Exigence | Cible mesurable | Vérifiée par |
|---|---|---|---|
| NFR-01 | Journée de collecte hors-ligne | ≥ 300 opérations sans réseau ; aucune fonction de collecte bloquée | E2E-01 |
| NFR-02 | Aucune perte d'opération | 0 perte après arrêt forcé de l'app, redémarrage du téléphone ou batterie vide, une fois le reçu affiché | Tests d'instrumentation, E2E-01 |
| NFR-03 | Exactement une fois | 0 doublon sous toutes les pannes réseau de `testing/toxiproxy` | E2E-02, tests par propriétés |
| NFR-04 | Sobriété en données | Envoi de 300 opérations ≤ 50 Ko compressés ; synchronisation quotidienne d'un portefeuille de 500 clients ≤ 300 Ko | Mesure automatique en CI |
| NFR-05 | Durabilité sur coupure électrique | 0 perte sur `kill -9` ou coupure simulée du nœud d'agence | E2E-11 |
| NFR-06 | Ordre et temps | Ordre par compte respecté ; dérive d'horloge détectée | Tests `nkulkap-sync` |
| NFR-07 | Autonomie d'agence | 72 h sans lien central | E2E-03 |
| NFR-08 | Appareils modestes | Android 8 (API 26), 2 Go de RAM ; démarrage à froid < 4 s ; APK < 40 Mo | Banc d'essai sur appareil de référence |
| NFR-09 | Bilinguisme | 100 % des écrans, SMS et documents clients en FR et EN ; contrôle en CI des clés manquantes | Lint i18n |
| NFR-10 | Sauvegarde et restauration | RPO ≤ 5 min ; RTO ≤ 4 h ; test de restauration mensuel automatisé | Runbook + tâche CI |
| NFR-11 | Disponibilité centrale | 99,5 % par mois, mono-serveur EMF | Supervision |
| NFR-12 | Empreinte | Pile complète d'une EMF de 50 000 clients sur un serveur de 8 vCPU et 32 Go de RAM **[À VALIDER par le test de charge, étape 12.3]** | k6 |
| NFR-13 | Performance | API Fineract p95 < 500 ms sous 50 utilisateurs simultanés ; `nkulkap-sync` ≥ 200 opérations/s comptabilisées | k6 |
| NFR-14 | Traçabilité | Toute écriture NKULKAP est reliée à son `op_id`, son auteur et son appareil | Revue + tests |
| NFR-15 | Observabilité | Traces distribuées de bout en bout (`traceparent` propagé de l'app à Fineract) ; tableaux de bord par composant | Revue |

---

