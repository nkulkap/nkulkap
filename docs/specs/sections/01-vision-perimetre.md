<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 69 à 108. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 1. Vision et périmètre

### 1.1 Problème

Les EMF camerounaises travaillent avec des logiciels souvent anciens et décentralisés. Ils supportent mal la collecte de terrain, le mobile money et le reporting COBAC. Le terrain impose trois réalités : réseau intermittent, coupures d'électricité, fraude interne sur la collecte d'espèces.

### 1.2 Cibles

| Cible | Priorité | Lignes concernées |
|---|---|---|
| EMF de 1re catégorie (coopératives, mutuelles) | 1 | L1 à L8 |
| EMF de 2e catégorie (sociétés anonymes) | 1 | L1 à L8 |
| EMF de 3e catégorie | 2 | L1 à L8 |
| Banques commerciales | 3 | L1 à L9 |

### 1.3 Les neuf lignes du périmètre

Ce périmètre reprend le tableau révisé validé avec le porteur de projet. La colonne « Écart » signale ce que la vérification a changé.

| # | Besoin | Langage | Composant | Écart par rapport au tableau validé |
|---|---|---|---|---|
| L1 | Plan comptable CEMAC + hors-bilan | Java | `custom/nkulkap/coa` | Hors-bilan réalisé **sans** modifier le cœur (ADR-003) |
| L2 | TEG, plafond réglementaire (taux d'usure) | Java | `custom/nkulkap/teg` | Aucun |
| L3 | Commissions agents, partage avec opérateurs | Numscript | `nkulkap-rules` | Numscript s'exécute dans un adaptateur Go sans état (ADR-005) |
| L4 | Collecte journalière, agent collecteur, tontine | Java + Kotlin | `custom/nkulkap/collecte`, `custom/nkulkap/tontine`, `nkulkap-field` | Aucun |
| L5 | Hors-ligne | Kotlin **+ Rust** | `nkulkap-field`, `nkulkap-sync`, `nkulkap-edge` | **Rust réintroduit** : l'app Mifos n'est pas idempotente (ADR-004) |
| L6 | MTN MoMo / Orange Money | Java | `nkulkap-pay` | **Sans Payment Hub EE** (ADR-007) |
| L7 | États COBAC, centrale des risques | SQL + Java | `custom/nkulkap/reporting` | Aucun |
| L8 | LAB-FT, fraude | Scala | `nkulkap-sentinel` | Aucun |
| L9 | Interbancaire CEMAC | Java | `nkulkap-pay` (adaptateurs) | Hors Payment Hub EE, comme L6 |

### 1.4 Hors périmètre de la version 1

- Libre-service client par USSD ou application client. Prévu en v2. Seul le « USSD push » des opérateurs, utilisé dans L6, est couvert.
- Cartes bancaires et monétique.
- Crédit-bail, affacturage, finance islamique.
- Transport des opérations par SMS quand il n'y a pas de données mobiles. C'est une piste v2, voir section 13.

---

