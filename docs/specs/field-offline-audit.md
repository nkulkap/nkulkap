# Audit du mode hors-ligne de l'app terrain (`apps/nkulkap-field`)

Étape 1.6, **partielle**. Audit par lecture du code le 2026-10-22, sur le fork `nkulkap/nkulkap-field` (fork d'`openMF/mifos-x-field-officer-app`, nouveau nom d'`android-client`), branche `dev`, commit `b5a57c95`, licence MPL-2.0. La compilation et le parcours sur émulateur sont reportés au poste puissant (`STATUS.md`, « À faire sur le poste puissant ») : les constats ci-dessous sont à confirmer à l'exécution.

Chemins relatifs à `apps/nkulkap-field`. L'app est en Kotlin Multiplatform (Compose, Room, Ktor + Ktorfit, Koin).

## Conclusion

**Absence d'idempotence confirmée.** Aucun appel ne porte d'`Idempotency-Key`, ni d'`externalId` d'opération : `core/network/.../MifosInterceptor.kt` (`install`) n'ajoute que `Content-Type`, `Accept`, `Fineract-Platform-TenantId` et `Authorization`. Les clés locales (entier auto-incrémenté, horodatage) ne sont jamais envoyées au serveur. Une réponse perdue après traitement par Fineract laisse donc la demande en base, avec un message d'erreur, et un nouvel envoi crée un doublon.

Défauts plus graves, découverts en lisant le code :

| # | Défaut | Emplacement | Effet |
|---|---|---|---|
| D1 | Après un remboursement réussi, la suppression vise la mauvaise table : `DELETE FROM LoanRepaymentTemplate WHERE loanId = :loanId` | `core/database/.../dao/LoanDao.kt:56-57` (`deleteLoanRepaymentByLoanId`), appelé par `LoanDaoHelper.deleteAndUpdateLoanRepayments` | La demande reste en base sans erreur. Le flux relu (`readAllLoanRepaymentTransaction`, observé en continu) relance `syncGroupPayload()`, qui renvoie la même demande : **remboursements en double, possiblement en boucle**. |
| D2 | Après une transaction d'épargne réussie, suppression de **toutes** les demandes du compte : `DELETE FROM SavingsAccountTransactionRequest where savingAccountId = :savingsAccountId` | `core/database/.../dao/SavingsDao.kt:48-49` | Deux dépôts hors-ligne sur un même compte : le second est **perdu** sans avoir été envoyé. |
| D3 | Clé primaire des remboursements = horodatage en secondes, insertion `OnConflictStrategy.REPLACE` | `LoanDaoHelper.kt:103` (`getCurrentTimeInMillis() / 1000`), `LoanDao.kt:32` | Deux remboursements saisis dans la même seconde : le premier est écrasé. |
| D4 | Mode hors-ligne = un seul booléen, `userStatus` (`true` = hors ligne, `Constants.USER_OFFLINE`), lu par chaque DataManager pour choisir entre réseau et Room | `core/datastore/.../UserPreferencesDataSource.kt:33`, `DataManagerLoan.submitPayment`, `DataManagerSavings.processTransaction`… | Une synchronisation lancée pendant que le drapeau vaut `true` réécrit la demande en local au lieu de l'envoyer. Le drapeau n'est remis à `false` que par l'écran de suivi de parcours (`PathTrackingScreen.kt`). |
| D5 | Écrans de synchronisation, drapeau à `true` : `TODO("Implement OfflineModeDialog()")` | 4 écrans de `feature/offline` : clients, groupes, centres, remboursements (l'écran d'épargne n'est pas concerné) | `NotImplementedError` : plantage. |
| D6 | Échec de création de client sans message d'erreur | `SyncClientPayloadsViewModel.kt:111-114` | L'élément est retenté à chaque passage ; doublon de client si le serveur l'avait créé. |
| D7 | Aucune synchronisation en arrière-plan (ni WorkManager ni worker) | — | La synchronisation est uniquement manuelle, écran par écran. |

Montants : stockés en `String` dans les entités (`transactionAmount`) : pas de flottant, mais aucun typage entier.

## Inventaire par flux

| Flux | Saisie hors-ligne (écran → stockage) | Entité Room (table, clé) | DAO / dépôt | Appel à la synchronisation | Après succès / échec |
|---|---|---|---|---|---|
| Tableau de bord | — | lit les 5 tables ci-dessous | `OfflineDashboardViewModel`, `OfflineDashboardRepositoryImp` | aucun (compteurs) | — |
| Clients | `CreateNewClientViewModel.kt:149-154`, `DataTableListViewModel.kt:180-185` → `DataManagerClient` | `ClientPayloadEntity` (`ClientPayload`, `id` auto) ; `DataTablePayload` (clé `clientCreationTime`) | `ClientDao.kt:121-143`, `SyncClientPayloadsRepositoryImp` | `POST clients` (`ClientService.kt:88-89`) | suppression par `id` / erreur non enregistrée (D6) |
| Groupes | `CreateNewGroupViewModel.kt:85-91` → `DataManagerGroups.createGroup` | `GroupPayloadEntity` (`GroupPayload`, `id` auto) | `GroupsDao.kt:38-60` | `POST groups` (`GroupService.kt:49-50`) | suppression par `id` / `errorMessage` |
| Centres | `CreateNewCenterViewModel.kt:60` → `DataManagerCenter.createCenter` | `CenterPayloadEntity` (`CenterPayload`, `id` auto) | `CenterDao.kt:39-52` | `POST centers` (`CenterService.kt:81-82`) | suppression par `id` / `errorMessage` |
| Remboursements de prêt | `LoanRepaymentViewModel.kt:125-130` → `DataManagerLoan.submitPayment` → `LoanDaoHelper.saveLoanRepaymentTransaction` | `LoanRepaymentRequestEntity` (`LoanRepaymentRequestEntity`, `timeStamp` en s) | `LoanDao.kt:32-57`, `SyncLoanRepaymentTransactionRepositoryImp` | `POST loans/{loanId}/transactions?command=repayment` (`LoanService.kt:76-80`) | mauvaise table supprimée (D1) / `errorMessage` |
| Transactions d'épargne | `SavingsAccountTransactionViewModel` → `DataManagerSavings.processTransaction` → `SavingsDaoHelper.saveSavingsAccountTransaction` | `SavingsAccountTransactionRequestEntity` (`SavingsAccountTransactionRequest`, `id` auto) | `SavingsDao.kt:33-52`, `SyncSavingsAccountTransactionRepositoryImp` | `POST {savingsAccountType}/{id}/transactions?command=deposit\|withdrawal` (`SavingsAccountService.kt:85-91`) | toutes les demandes du compte supprimées (D2) / `errorMessage` |

Base : `core/database/src/androidMain/kotlin/com/mifos/room/MifosDatabase.kt`, version 3, `AutoMigration` 1→2 et 2→3, schéma exporté. HTTP : Ktor (OkHttp sur Android), délais de 60 s (`KtorHttpClient.android.kt`), sans politique de nouvelle tentative.

## Fichiers à modifier en phase 7

Règle du projet : le code NKULKAP des apps MPL va dans des **fichiers nouveaux** ; les fichiers existants ne reçoivent que les branchements minimaux.

| Besoin (ADR-008, ADR-016) | Fichiers existants touchés | Fichiers nouveaux prévus |
|---|---|---|
| `op_id` UUIDv7 créé à la saisie, persisté avant tout réseau | les 2 entités de transaction et `ClientPayloadEntity`, `GroupPayloadEntity`, `CenterPayloadEntity` (colonne `opId`) ; `MifosDatabase.kt` (version 4, migration) | générateur UUIDv7 ; migration Room 3→4 |
| Envoi de l'`Idempotency-Key` et de l'`externalId` = `op_id` | `MifosInterceptor.kt` ou les services Ktorfit concernés (`LoanService`, `SavingsAccountService`, `ClientService`, `GroupService`, `CenterService`) | intercepteur NKULKAP qui pose l'en-tête depuis la requête |
| Suppression ciblée après succès | `LoanDao.kt` (D1), `SavingsDao.kt` (D2), `LoanDaoHelper.kt`, `SavingsDaoHelper.kt` | — |
| Clé locale stable | `LoanRepaymentRequestEntity.kt`, `LoanDaoHelper.kt:103` (D3) | — |
| Remplacement de la synchronisation manuelle par le protocole `nkulkap-sync` (pull/push JSON+gzip v1) | `feature/offline/*ViewModel.kt` (6), DataManagers (`DataManagerLoan`, `DataManagerSavings`, `DataManagerClient`, `DataManagerGroups`, `DataManagerCenter`) | client de synchronisation, file d'opérations, worker de fond |
| Mode hors-ligne fiable (D4, D5) | `UserPreferencesDataSource.kt`, les 4 écrans de D5 | état de connectivité NKULKAP |
| Classement des réponses (succès, refus définitif, rejeu) | les 6 ViewModels de synchronisation | classeur d'erreurs selon ADR-016 |

## Reste à faire (poste puissant)

- Compiler `apps/nkulkap-field` (`./gradlew :cmp-android:assembleDebug`, à confirmer) et le lancer sur émulateur contre la pile locale.
- Parcourir chaque flux hors-ligne et confirmer D1 à D6 à l'exécution, notamment la boucle de D1.
