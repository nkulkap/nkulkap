<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 842 à 876. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 2 — Socle commun

**Étape 2.1 — Contrats**
- **Faire** : `contracts/sync-v1.openapi.yaml` (annexe C), `pay-v1`, `sentinel-v1` et les schémas d'événements NKULKAP (`op.posted`, `op.rejected`, `payment.*`, `tournee.*`). Génération des clients et serveurs là où c'est utile.
- **Terminé quand** : les contrats sont validés par un linter OpenAPI en CI.

**Étape 2.2 — Générateur de données fictives**
- **Faire** : générateur reproductible (graine fixe) selon la section 10.4, avec chargement dans Fineract par API.
- **Terminé quand** : `make fixtures SIZE=small|large` produit un jeu identique à chaque exécution.

**Étape 2.3 — Harnais de réseau dégradé**
- **Faire** : `testing/toxiproxy/` : profils de la section 10.2, utilitaire de bascule de profil utilisable depuis les tests Rust, Java, Scala et les scripts E2E, et mode `reponse_perdue`.
- **Terminé quand** : un test démontre qu'une requête réussie côté serveur ne reçoit pas de réponse en mode `reponse_perdue`.

**Étape 2.4 — Identité**
- **Faire** : royaume Keycloak `nkulkap` (rôles de la section 9.1, clients `nkulkap-web`, `nkulkap-field`, `nkulkap-sync`, `nkulkap-pay`, `nkulkap-sentinel`), Fineract configuré en serveur de ressources OAuth2, double authentification pour comptable, conformité et administrateur. Export du royaume versionné.
- **Terminé quand** : un utilisateur « collecteur » obtient un jeton et appelle `GET /nkulkap/ping` ; un collecteur est refusé sur une ressource de comptable.

**Étape 2.5 — Observabilité**
- **Faire** : OpenTelemetry dans chaque service, propagation de `traceparent`, tableaux de bord Grafana de base.
- **Terminé quand** : une requête de synchronisation est visible de bout en bout dans une seule trace.

**Étape 2.6 — Bibliothèque commune NKULKAP (Java)**
- **Faire** : dans `custom/nkulkap/common/core` :
  - `Money` (XAF, montant `long`) ;
  - générateur UUIDv7 ;
  - table et service d'audit `nkulkap_audit` ;
  - fichiers de messages i18n FR et EN ;
  - catalogue de codes d'erreur ;
  - client `nkulkap-rules` ;
  - constructeur d'écritures comptables équilibrées (FR-L3-04).
- **Terminé quand** : couverture des tests ≥ 90 % sur ce module.

---

