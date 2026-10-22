# Module custom NKULKAP dans Fineract

Étapes 1.4 et 1.5. Vérifié le 2026-10-22 dans l'image `fineract-custom` construite par `./gradlew :custom:docker:jibDockerBuild` (Fineract `b94b2ddf`).

## Mécanismes d'extension utilisés (1.4)

| Besoin | Mécanisme | Fichier NKULKAP |
|---|---|---|
| Inclusion dans le build et l'image | Tout dossier `custom/<société>/<catégorie>/<module>` doté d'un `build.gradle` devient le projet `:custom:<société>:<catégorie>:<module>` et entre dans l'image (`custom/docker/build.gradle`). | `core/custom-nkulkap/common/{core,starter}/build.gradle` |
| Chargement Spring | Auto-configuration déclarée dans `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`, qui balaie le paquet du module. | `common/starter` : `NkulkapCommonAutoConfiguration` (`@ComponentScan("cm.nkulkap.fineract.common")`) |
| Ressource REST | `JerseyConfig` enregistre tout bean Spring annoté `@Path` ; le chemin est préfixé par `/api`. Authentification Fineract habituelle (401 sans identifiants valides). | `NkulkapPingApiResource`, `@Path("/v1/nkulkap/ping")` |
| Migration | Liquibase inclut `db/custom-changelog/*` de tous les jars (contexte `tenant_db`), par ordre alphabétique de nom de fichier, pour chaque locataire. | `0001_nkulkap_common_ping.xml` : table `nkulkap_ping`, une ligne `nkulkap-common` |
| Accès base | `JdbcTemplate` de Fineract, routé vers la base du locataire courant. | `NkulkapPingRepository` |

`GET /api/v1/nkulkap/ping` → `200 {"status":"ok","module":"nkulkap-common"}` ; la valeur `module` est lue dans `nkulkap_ping`, ce qui prouve à la fois le chargement du module et le passage de la migration.

Dépendances à déclarer dans un module NKULKAP : celles de `fineract-core` sont en `implementation`, donc non transitives (`jakarta.ws.rs-api`, `jakarta.annotation-api`, `spring-jdbc`…). Un module qui manipule une entité JPA de Fineract (ex. `Loan`) doit aussi déclarer `spring-boot-starter-data-jpa` (sans Hibernate) et `org.eclipse.persistence.jpa` (sans `jakarta.persistence`), sinon la compilation échoue sur `Auditable` ou `PersistenceWeaved`.

## Veto avant décaissement (1.5)

`DisbursalVetoSpikeListener` (`common/core`, paquet `veto`) s'enregistre au démarrage par `BusinessEventNotifierService.addPreBusinessEventListener(LoanDisbursalBusinessEvent.class, this)`. Il lève `GeneralPlatformDomainRuleException("error.msg.nkulkap.loan.disbursal.vetoed", …)` pour un prêt dont l'`externalId` commence par `TEST-VETO`. Il n'est actif que si `nkulkap.spike.disbursal-veto.enabled=true` (variable `NKULKAP_SPIKE_DISBURSAL_VETO_ENABLED` de `deploy/compose/env/fineract.env`).

L'événement « pre » est émis par `LoanWritePlatformServiceJpaRepositoryImpl.disburseLoan` dans la transaction de la commande, avant la création de la transaction de décaissement et des écritures.

Observé (`DisbursalVetoSpikeIT`) :

| Prêt | Réponse | État après |
|---|---|---|
| `TEST-VETO-…` | `403`, `userMessageGlobalisationCode` = `validation.msg.domain.rule.violation`, `errors[0].userMessageGlobalisationCode` = `error.msg.nkulkap.loan.disbursal.vetoed`, `developerMessage` repris de l'exception | statut `Approved`, aucune transaction, aucune écriture (`GET /journalentries?loanId=` : 0), pas de date de décaissement : annulation complète |
| témoin | `200` | statut `Active`, 2 lignes d'écriture |

Le code d'erreur arrive au client de l'API dans `errors[0].userMessageGlobalisationCode` : c'est lui que les satellites traduisent (fr, en). Le point d'extension convient donc aux vetos métier (TEG et usure en phase 4, LAB-FT en phase 10) sans modifier le cœur ; aucun ADR correctif n'est nécessaire.

Remarque : avec une `Idempotency-Key`, le refus est mis en cache sous cette clé (voir [idempotence-fineract.md](idempotence-fineract.md), cas f).
