<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 762 à 793. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 0 — Préparer

**Étape 0.1 — Créer le monodépôt**
- **Faire** : créer l'arborescence de la section 6, `README.md`, `Makefile` (cibles `bootstrap`, `build`, `test`, `e2e`, `up`, `down`, `logs`), `CLAUDE.md` (annexe A), `docs/STATUS.md` (tableau des phases et étapes, colonne statut), `.editorconfig`, `.gitignore`.
- **Vérifier** : `make help` liste les cibles.
- **Terminé quand** : l'arborescence existe ; `CLAUDE.md` et `docs/STATUS.md` sont présents ; premier commit fait.

**Étape 0.2 — Fork de Fineract et modules custom**
- **Faire** :
  1. Créer le fork `nkulkap-fineract` à partir du commit de référence (ou de la dernière étiquette de version stable si elle est postérieure).
  2. L'ajouter en sous-module dans `core/fineract`.
  3. Créer `core/custom-nkulkap/common/core` avec un `build.gradle` calqué sur `custom/acme/note/service`.
  4. Créer le lien symbolique `core/fineract/custom/nkulkap → ../../custom-nkulkap` dans `make bootstrap`.
- **Vérifier** : `cd core/fineract && ./gradlew projects | grep custom:nkulkap`, puis `./gradlew :custom:nkulkap:common:core:build`.
- **Terminé quand** : Gradle découvre les modules NKULKAP sans modifier `settings.gradle`.
- **Si le lien symbolique pose problème** (Windows, outils) : remplacer par une copie synchronisée dans `make bootstrap` et le consigner dans un ADR.

**Étape 0.3 — ADR**
- **Faire** : écrire ADR-001 à ADR-012 (section 3) dans `docs/adr/`, au format court : contexte, décision, conséquences.
- **Terminé quand** : 12 fichiers présents et liés depuis `docs/adr/README.md`.

**Étape 0.4 — Pile de développement**
- **Faire** : `deploy/compose/dev.yml`, en partant de `docker-compose-postgresql-kafka.yml` du dépôt Fineract. Services : PostgreSQL, Kafka (KRaft), Keycloak, Fineract (image `fineract-custom` construite par `./gradlew :custom:docker:jibDockerBuild`), Toxiproxy, Grafana, Prometheus et Loki (profil `obs` optionnel). Contrôles de santé sur chaque service. Le fuseau du locataire Fineract est réglé sur `Africa/Douala`.
- **Vérifier** : `make up`, puis `curl -k -H "Fineract-Platform-TenantId: default" -u mifos:password https://localhost:8443/fineract-provider/api/v1/offices` renvoie 200.
- **Terminé quand** : la pile démarre en moins de 5 minutes sur un poste de 16 Go de RAM ; `make down` nettoie tout.

**Étape 0.5 — Intégration continue**
- **Faire** : un workflow par composant (déclenché par chemin modifié) et un workflow E2E nocturne (vide pour l'instant). Mise en cache Gradle, Cargo, sbt et Go.
- **Terminé quand** : un commit sur `core/custom-nkulkap` déclenche seulement la chaîne de Fineract custom, et elle passe.

---

