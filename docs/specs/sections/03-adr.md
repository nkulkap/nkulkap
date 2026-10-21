<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 132 à 174. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 3. Décisions d'architecture (ADR)

Chaque ADR est à recopier dans `docs/adr/NNNN-titre.md` à l'étape 0.3.

**ADR-001 — Fineract est la seule source de vérité comptable.**
Les satellites (`nkulkap-sync`, `nkulkap-pay`, `nkulkap-rules`, `nkulkap-sentinel`, `nkulkap-edge`) ne tiennent aucun solde officiel. Ils écrivent dans Fineract par son API REST, avec l'en-tête `Idempotency-Key`. Fineract le prend en charge nativement : voir `IdempotencyKeyResolver` et la propriété `fineract.idempotency-key-header-name`.

**ADR-002 — Le code NKULKAP côté Fineract vit dans `custom/nkulkap/<domaine>/<module>`.**
Fineract inclut automatiquement les modules qui suivent le motif `custom/<société>/<catégorie>/<module>` (voir `settings.gradle`). Ses migrations vont dans `db/custom-changelog`. Ses ressources REST sont des beans annotés `@Path`, enregistrés par `JerseyConfig`. Limite connue : la documentation de Fineract qualifie encore les modules personnalisés de « proof of concept ». Les modules NKULKAP ajoutent donc des fonctions nouvelles plutôt que de remplacer des services existants.

**ADR-003 — Le hors-bilan se gère par des comptes étiquetés, sans modifier `GLAccountType`.**
`GLAccountType` ne connaît que 5 types (ASSET, LIABILITY, EQUITY, INCOME, EXPENSE), dans `fineract-core`. Les comptes de classe 9 du PCEMF sont donc créés comme paires de comptes ASSET et LIABILITY qui se compensent. Ils portent l'étiquette `HORS_BILAN`, via les valeurs de code d'étiquettes de comptes de Fineract. Ils sont exclus du bilan par le moteur de reporting (L7). Un changement du cœur pourra être proposé en amont plus tard.

**ADR-004 — Rust pour `nkulkap-sync` et `nkulkap-edge`.**
Constat d'audit : l'app `openMF/android-client` envoie chaque opération hors-ligne directement à l'API Fineract, sans clé d'idempotence (0 occurrence dans le code Kotlin). En cas d'erreur, elle garde l'opération et la renvoie. Une réponse perdue après enregistrement provoque donc un doublon. Il faut une passerelle qui garantisse l'exactement-une-fois, et un nœud d'agence qui tourne des jours sur du matériel modeste sans surveillance.
Pourquoi Rust : binaire statique unique (cible `musl`, x86_64 et aarch64), empreinte mémoire faible et stable, pas de ramasse-miettes, sûreté mémoire, concurrence sûre pour des files de messages critiques.

**ADR-005 — Numscript pour les répartitions, exécuté par un adaptateur Go sans état.**
Numscript (licence MIT, `github.com/formancehq/numscript`) est une bibliothèque Go. `nkulkap-rules` est un service Go minimal, de l'ordre de 300 lignes de code applicatif, qui charge des scripts `.num`, les exécute avec la source `allowing unbounded overdraft` (calcul pur, sans solde) et renvoie des écritures. On n'utilise jamais le ledger Formance : Fineract reste la seule comptabilité (ADR-001). La logique métier vit dans les fichiers `.num`, lisibles et modifiables sans recompiler. Exemples validés en annexe B.

**ADR-006 — Scala 3 pour `nkulkap-sentinel`.**
Fineract publie ses événements métier sur Kafka au format Avro (schémas dans `fineract-avro-schemas`). Scala 3, avec Cats Effect et fs2-kafka, convient au traitement de flux à états (fenêtres glissantes, vélocité, fractionnement) et à l'expression de règles sous forme de fonctions pures testables par propriétés.

**ADR-007 — `nkulkap-pay` en Java, sans Payment Hub EE.**
Payment Hub EE épingle Zeebe 8.2.8 (`ph-ee-env-template`), une version ancienne. Depuis Camunda 8.6, l'usage en production de Camunda 8 Self-Managed, Zeebe compris, exige une licence payante. PH-EE impose en plus Kubernetes et Elasticsearch, trop lourds pour une EMF. `nkulkap-pay` est donc un service Spring Boot avec machine à états en base PostgreSQL, boîte d'envoi transactionnelle (*outbox*) et tâches planifiées. Son interface interne suit le vocabulaire de la GSMA Mobile Money API pour permettre une intégration ultérieure.

**ADR-008 — Protocole de synchronisation : JSON + gzip, versionné (v1).**
Le format reste lisible pour le débogage et gzip est natif sur Android. Si les mesures dépassent le budget de NFR-04, on passe à Protocol Buffers en v2.

**ADR-009 — PostgreSQL comme base unique, Apache Kafka en mode KRaft.**
Un seul SGBD à exploiter, supporté par Fineract. Kafka est le transport des événements externes de Fineract.

**ADR-010 — Keycloak (OIDC) pour l'identité.**
Utilisateurs du back-office, collecteurs, appareils. Fineract en mode serveur de ressources OAuth2. Double authentification pour les rôles sensibles.

**ADR-011 — Hébergement au Cameroun par défaut.**
La loi n° 2024/017, applicable depuis le 23 juin 2026, soumet le transfert de données personnelles à l'étranger à une autorisation préalable **[À VÉRIFIER : texte officiel]**. L'offre SaaS est donc hébergée dans un centre de données au Cameroun, et l'installation sur site reste possible.

**ADR-012 — Modèle de multi-location.**
Fineract est multi-locataire, avec une base par locataire. Une petite EMF partage l'instance SaaS. Une grande EMF ou une banque dispose de sa propre instance. Tous les satellites portent l'identifiant de locataire (`Fineract-Platform-TenantId`).

---

