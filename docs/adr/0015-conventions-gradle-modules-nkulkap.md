# ADR-015 — Conventions Gradle propres aux modules NKULKAP : formatage et en-tête de licence

- Statut : Accepté
- Date : 2026-10-22
- Source : étape 0.5 ; panne de download.eclipse.org du 2026-10-22 ; choix d'une licence propriétaire

## Contexte

Fineract applique à tous ses sous-projets, donc aussi à `custom/nkulkap/**`, une configuration Spotless qui :
- impose et insère l'en-tête de licence de la fondation Apache sur les fichiers Java, XML, `.properties`, `.vm` et `.jsp` ;
- formate Java et Gradle avec les formateurs Eclipse 4.29, téléchargés depuis `download.eclipse.org` (dépôt P2).

Le code NKULKAP est propriétaire (Circaura Sarl) : l'en-tête Apache y serait faux. Le 2026-10-22, `download.eclipse.org` est en arrêt temporaire, et aucun miroir ne sert la version 4.29 : le formatage de Fineract ne peut plus s'exécuter. La première réaction, sauter Spotless en CI, laissait passer du code non vérifié sans le dire.

## Décision

Chaque module NKULKAP applique `core/custom-nkulkap/nkulkap-conventions.gradle`, qui remplace pour nos seuls modules (`clearSteps()`), sans modifier Fineract :
- Java : `importOrder`, `removeUnusedImports`, palantir-java-format (version épinglée, distribuée par Maven Central), en-tête propriétaire Circaura Sarl ;
- Gradle : règles d'espacement de Fineract, sans le formateur Eclipse ;
- XML, `.properties`, `.vm`, `.jsp` : sans en-tête Apache ;
- audit Apache RAT désactivé.

En CI, le cache `~/.gradle` (dépendances, formateurs, données P2) est conservé entre les exécutions. Spotless n'est jamais sauté : si un téléchargement échoue, le job échoue et une étape de diagnostic indique la cause.

## Conséquences

- Le formatage des modules NKULKAP ne dépend plus de `download.eclipse.org`.
- Le style Java de nos modules (palantir, 120 colonnes) diffère de celui du cœur de Fineract (Eclipse) ; les deux codes vivent dans des dossiers distincts.
- Tout nouveau module doit appliquer le script de conventions (rappel dans `core/custom-nkulkap/README.md`).
- Checkstyle et SpotBugs de Fineract restent actifs sur nos modules.
