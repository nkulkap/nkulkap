<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 680 à 734. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 10. Stratégie de tests

### 10.1 Pyramide

| Niveau | Outils | Contenu |
|---|---|---|
| Unitaires | JUnit 5, jqwik, `cargo test` + proptest, munit + ScalaCheck, `go test`, tests Kotlin | Calculs (TEG, frais, répartitions), machines à états, analyseur de formules de reporting |
| Par propriétés | jqwik, proptest, ScalaCheck | Somme des répartitions = montant ; synchronisation : toute permutation ou duplication de lots donne le même état final ; TEG monotone en fonction des frais |
| Intégration | Testcontainers (PostgreSQL, Kafka, Fineract) | Modules custom chargés dans un vrai Fineract ; `nkulkap-sync` contre un vrai Fineract |
| Contrats | Fichiers OpenAPI de `contracts/` + validation des requêtes et réponses | `nkulkap-field` ↔ `nkulkap-sync`, `nkulkap-web` ↔ API NKULKAP |
| Simulateurs | WireMock (MTN, Orange) | Rappel retardé, en double, absent ; PENDING sans fin ; erreurs 500 ; délais |
| Réseau dégradé | Toxiproxy | Profils 2G et 3G, perte de paquets, réponse perdue après enregistrement, coupure en plein transfert |
| E2E | Scénarios `testing/e2e` (Docker Compose + émulateur Android en CI nocturne) | E2E-01 à E2E-12 |
| Charge | k6 | NFR-12 et NFR-13 |

### 10.2 Profils réseau (`testing/toxiproxy/profiles.yaml`)

| Profil | Latence | Gigue | Débit | Perte simulée | Usage |
|---|---|---|---|---|---|
| `edge_rural` | 600 ms | 300 ms | 40 kbit/s | Coupure de connexion 10 % | Collecteur en brousse |
| `3g_urbain` | 150 ms | 80 ms | 1 Mbit/s | 1 % | Ville |
| `vsat_agence` | 700 ms | 100 ms | 512 kbit/s | 2 % | Agence rurale |
| `reponse_perdue` | — | — | — | Coupure **après** envoi complet de la requête, avant la réponse | Test d'idempotence |
| `coupure_longue` | — | — | — | Lien coupé 72 h (temps simulé) | E2E-03 |

> Toxiproxy n'a pas de « perte de paquets » au sens strict. On la simule avec les toxiques `timeout`, `reset_peer` et `limit_data` appliqués aléatoirement par le harnais de test.

Émulateur Android : `adb emu network speed edge` et `adb emu network delay gprs`. Bascules du mode avion scriptées pendant les tests.

### 10.3 Scénarios de bout en bout

| ID | Scénario | Résultat attendu |
|---|---|---|
| E2E-01 | Collecteur : téléchargement du portefeuille, réseau coupé, 300 collectes, arrêt forcé de l'app, redémarrage du téléphone, puis synchronisation en profil `edge_rural` | 300 dépôts comptabilisés exactement une fois ; 300 SMS ; tournée rapprochée avec écart 0 ; commission répartie par Numscript ; écritures équilibrées |
| E2E-02 | 50 opérations envoyées sous le profil `reponse_perdue` | 0 doublon dans Fineract ; statuts corrects sur l'appareil |
| E2E-03 | Agence coupée 72 h : guichet en mode dégradé, retraits dans les plafonds, un retrait en solde insuffisant au retour | File vidée dans l'ordre ; retrait en revue ; approuvé par `force-withdrawal` ; trace d'audit complète |
| E2E-04 | Dépôt MoMo (`requestToPay`) dont le rappel est perdu | L'interrogation périodique résout ; épargne créditée une seule fois |
| E2E-05 | Retrait vers MoMo refusé par l'opérateur | Contre-passation ; solde rétabli ; SMS d'échec |
| E2E-06 | Prêt dont le TEG dépasse l'usure | Décaissement bloqué ; fiche TEG générée |
| E2E-07 | Tontine de 10 membres sur 10 cycles, 1 retard | Pénalité appliquée ; chaque membre servi une fois ; cagnotte à 0 à la fin |
| E2E-08 | Fin de mois : clôture puis génération des états | Balance équilibrée ; hors-bilan exclu du bilan ; hachages reproductibles |
| E2E-09 | Fractionnement en 5 dépôts sous le seuil ; client présent sur la liste ONU | Alerte puis dossier puis brouillon de déclaration ; débit bloqué |
| E2E-10 | Client anglophone | SMS et fiche TEG en anglais |
| E2E-11 | `kill -9` et coupure simulée du disque de `nkulkap-edge` pendant 100 écritures | 0 perte ; reprise automatique |
| E2E-12 | Révocation d'un téléphone volé | Lots refusés ; effacement local à la reconnexion |

### 10.4 Données de test

`testing/fixtures` contient un générateur de données **fictives** :
- noms camerounais variés (francophones et anglophones) ;
- numéros `+2376…` dans des plages réservées aux tests ;
- 10 agences, 50 collecteurs, 20 000 clients, 12 mois d'historique.

Tout jeu de données porte la mention `FICTIF`. Aucune donnée réelle de client n'entre dans les dépôts.

