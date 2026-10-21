<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 109 à 131. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 2. Contraintes du terrain camerounais

Chaque contrainte devient une exigence testable. C'est la raison d'être de Rust, Scala et Numscript dans ce projet.

| # | Contrainte | Conséquence concrète | Réponse NKULKAP | Composants | Exigence |
|---|---|---|---|---|---|
| C1 | Réseau 2G/3G intermittent, zones blanches rurales | Un collecteur peut passer une journée entière sans données | Fonctionnement hors-ligne d'abord : persistance locale, puis synchronisation asynchrone | `nkulkap-field`, `nkulkap-sync` | NFR-01, NFR-02 |
| C2 | Réponse perdue après enregistrement côté serveur | L'app renvoie, et le serveur comptabilise deux fois | Idempotence de bout en bout : `op_id` = en-tête `Idempotency-Key` de Fineract | `nkulkap-sync`, `nkulkap-pay` | NFR-03 |
| C3 | Données mobiles chères, débit faible | Un portefeuille complet ne peut pas se retélécharger chaque jour | Synchronisation différentielle, compression gzip, budget de données, priorité au Wi-Fi d'agence | `nkulkap-sync`, `nkulkap-field` | NFR-04 |
| C4 | Délestages électriques | Un serveur d'agence ou un téléphone peut s'éteindre en pleine écriture | Écritures durables (SQLite WAL, `fsync`), reprise automatique, onduleur | `nkulkap-edge`, `nkulkap-field` | NFR-05 |
| C5 | Horloges de téléphone fausses | La date de valeur est faussée et l'ordre des opérations est incertain | La date serveur fait foi ; horloge logique hybride (HLC) pour l'ordre ; rejet au-delà d'une dérive fixée | `nkulkap-sync` | NFR-06 |
| C6 | Agences sur liaisons lentes ou coupées (VSAT, 3G) | Le guichet s'arrête quand le lien tombe | Nœud d'agence avec mode dégradé et plafonds hors-ligne | `nkulkap-edge` | NFR-07 |
| C7 | Fraude sur la collecte d'espèces | Collecte non reversée, montants modifiés | Reçu immédiat avec code, SMS au client, GPS, rapprochement quotidien de tournée, alertes | `nkulkap-field`, `custom/nkulkap/collecte`, `nkulkap-sentinel` | FR-L4-07, FR-L8-05 |
| C8 | Mobile money : rappels perdus, statuts en attente prolongés, doublons | Argent débité chez l'opérateur mais absent du compte | Machine à états, interrogation périodique, rapprochement J+1 | `nkulkap-pay` | FR-L6-03 à 06 |
| C9 | Téléphones d'entrée de gamme | Lenteurs, plantages, mémoire saturée | Android 8 (API 26) minimum, cible 2 Go de RAM, tests sur appareil bas de gamme | `nkulkap-field` | NFR-08 |
| C10 | Bilinguisme officiel FR/EN | Clients anglophones (Nord-Ouest, Sud-Ouest) | i18n partout, y compris SMS et documents clients | Tous | NFR-09 |
| C11 | Clients sans smartphone, faible littératie numérique | Pas d'app client possible | Canaux assistés par agent, USSD push opérateur, reçus SMS en GSM-7 | `nkulkap-pay`, `nkulkap-field` | FR-L6-02 |
| C12 | Peu de personnel informatique dans les EMF | Mises à jour et sauvegardes négligées | Déploiement packagé, mises à jour signées, sauvegardes automatiques hors site, supervision par l'éditeur | Infra | NFR-10, NFR-11 |
| C13 | Réglementation COBAC, BEAC, ANIF ; loi n° 2024/017 sur les données personnelles | Sanctions, retrait d'agrément | Modules de conformité, hébergement au Cameroun par défaut, journal d'audit | L1, L2, L7, L8, infra | Section 9 |
| C14 | Budget informatique serré | Pas de grappe Kubernetes chez une petite EMF | Pile complète d'une EMF sur un seul serveur | Infra | NFR-12 |

---

