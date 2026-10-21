<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 244 à 296. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 5. Stack technologique complet

Politique de versions : on épingle la version exacte dans chaque fichier de build au démarrage du projet (dernière version stable), puis on la met à jour par Renovate. Les versions ci-dessous sont celles constatées ou les minimums requis.

### 5.1 Langages et rôles

| Langage | Version | Où | Pourquoi |
|---|---|---|---|
| Java | 25 (toolchain imposée par Fineract) | `nkulkap-core` (modules custom), `nkulkap-pay` | Langue de Fineract, vivier de développeurs au Cameroun |
| Kotlin | 2.2.x (version de `android-client`) | `nkulkap-field` | Base Kotlin Multiplatform existante |
| Rust | Stable, édition 2024 | `nkulkap-sync`, `nkulkap-edge` | Fiabilité, binaire unique, faible empreinte (ADR-004) |
| Scala | Scala 3 (branche LTS) | `nkulkap-sentinel` | Traitement de flux, règles en fonctions pures (ADR-006) |
| Numscript | Bibliothèque `formancehq/numscript` (MIT) | `rules/*.num` | Répartitions lisibles (ADR-005) |
| Go | ≥ 1.24 (selon la version de Numscript épinglée) | `nkulkap-rules` (adaptateur seulement) | Imposé par la bibliothèque Numscript |
| TypeScript | Version de `openMF/web-app` | `nkulkap-web` | Base Angular existante |
| SQL | PostgreSQL | Rapports Fineract (« stretchy reports »), requêtes de reporting | Natif dans Fineract |

> Sept langages au total (Java, Kotlin, Rust, Scala, Go, Numscript, TypeScript), plus SQL. Le cœur métier reste Java et Kotlin. Rust, Scala, Go et Numscript sont confinés à des services aux frontières nettes, pour qu'un départ de développeur ne bloque qu'un composant.

### 5.2 Bibliothèques par composant

| Composant | Bibliothèques principales |
|---|---|
| `nkulkap-core` (custom Fineract) | Spring Boot (version de Fineract), Liquibase (`db/custom-changelog`), Jersey (JAX-RS), Apache POI (XLSX), Apache PDFBox (fiche TEG), JUnit 5, Cucumber (déjà inclus pour les modules custom), Mockito |
| `nkulkap-pay` | Spring Boot, Spring JDBC, Flyway, Resilience4j (délais, nouvelles tentatives, disjoncteur), ShedLock (tâches planifiées en grappe), client Fineract `fineract-client-feign`, WireMock (simulateurs), Testcontainers, jqwik |
| `nkulkap-sync` | tokio, axum, sqlx (PostgreSQL), rdkafka, serde/serde_json, async-compression (gzip), ed25519-dalek, uuid (v7), reqwest, tracing + OpenTelemetry, proptest |
| `nkulkap-edge` | tokio, axum, sqlx (SQLite, mode WAL), askama (gabarits HTML) + htmx (interface du guichet), escpos (imprimante de reçus), ed25519-dalek (vérification des mises à jour), proptest |
| `nkulkap-rules` | `github.com/formancehq/numscript`, `net/http` standard, YAML (`account_map.yaml`) |
| `nkulkap-sentinel` | Cats Effect 3, fs2-kafka + vulcan (Avro), http4s, doobie, Flyway, circe, Apache Commons Text (Jaro-Winkler), munit + munit-cats-effect, ScalaCheck, testcontainers-scala |
| `nkulkap-field` | Base `openMF/android-client` : Compose Multiplatform, Room, Ktor ; ajouts : WorkManager (synchronisation en arrière-plan), chiffrement de la base locale (ADR à rédiger à l'étape 7.6), localisation GPS |
| `nkulkap-web` | Base `openMF/web-app` : Angular, Angular Material, traductions `fr-FR` et `en-US` existantes |

### 5.3 Infrastructure et outillage

| Domaine | Choix |
|---|---|
| Base de données | PostgreSQL (version supportée par Fineract à la date du démarrage) |
| Messagerie | Apache Kafka, mode KRaft (sans ZooKeeper), un seul nœud pour une EMF seule |
| Identité | Keycloak (OIDC), double authentification pour les rôles sensibles |
| Conteneurs | Docker ; image Fineract construite avec `./gradlew :custom:docker:jibDockerBuild` |
| Déploiement mono-EMF | Docker Compose sur un serveur (NFR-12) |
| Déploiement SaaS | Kubernetes léger (k3s) + Helm |
| Nœud d'agence | Mini-PC x86_64 ou ARM64, SSD, onduleur, Linux, binaire `nkulkap-edge` géré par systemd |
| Observabilité | OpenTelemetry, Prometheus, Grafana, Loki |
| Sauvegardes | pgBackRest : sauvegarde complète quotidienne, archivage WAL continu, copie hors site au Cameroun |
| Secrets | SOPS + age dans le dépôt de configuration ; jamais de secret en clair |
| CI | GitHub Actions (ou GitLab CI), une chaîne de build par composant + E2E nocturne |
| Simulation réseau | Toxiproxy (latence, débit, coupures, réponses perdues) ; profils réseau de l'émulateur Android (`gsm`, `edge`) |
| Charge | k6 (outil interne uniquement) |
| Mises à jour de dépendances | Renovate |

---

