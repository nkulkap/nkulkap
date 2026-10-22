# ADR-014 — Intégration continue sur GitHub Actions, avec bascule prévue vers TeamCity

- Statut : Accepté
- Date : 2026-10-22
- Source : étape 0.5 du cahier des charges ; décision du porteur du projet

## Contexte

Le cahier prévoit GitHub Actions (ou GitLab CI), avec une chaîne de build par composant et un E2E nocturne. Le porteur du projet souhaite utiliser TeamCity à terme, et veut éviter de découvrir tard des blocages dus à la facturation.

Les dépôts `nkulkap/nkulkap` et `nkulkap/nkulkap-fineract` sont publics. Pour un dépôt public, GitHub Actions est gratuit et sans quota de minutes sur les machines standard. Le quota gratuit de 2 000 minutes par mois ne s'applique qu'aux dépôts privés : il représenterait environ 80 à 130 builds de Fineract, trop peu pour une équipe de 4 à 6 ingénieurs. Sans moyen de paiement, un dépassement arrête les builds sans facturer.

TeamCity demande d'héberger et d'entretenir un serveur et des agents. Sa version gratuite sur site est limitée en agents et en configurations. Exécuter sur nos propres agents le code de pull requests venues d'un dépôt public est un risque de sécurité.

## Décision

L'intégration continue tourne sur GitHub Actions tant que les dépôts sont publics.

On bascule vers TeamCity sur site dès que l'un de ces cas se présente :
- les dépôts passent en privé ;
- le quota gratuit ne suffit plus ;
- on met en place le déploiement continu vers le centre de données camerounais (phase 12), ou des E2E lourds sur nos propres machines.

Pour que la bascule ne demande aucune réécriture, les workflows n'appellent que des cibles `make` (`make bootstrap`, `make build`, `make test COMPONENT=…`). La logique de build vit dans le `Makefile` et les outils de chaque composant, jamais dans le YAML de la CI.

## Conséquences

- Aucun serveur de CI à exploiter pendant les premières phases.
- Les workflows restent minces : préparation de l'outillage, cache, puis une cible `make`. TeamCity reprendra les mêmes commandes, décrites en Kotlin DSL dans `.teamcity/`.
- Une pull request venue d'un fork ne s'exécute que sur les machines jetables de GitHub, jamais sur une infrastructure NKULKAP.
- Avant tout passage des dépôts en privé, la bascule vers TeamCity doit être prête, faute de quoi la CI s'arrêtera au premier dépassement de quota.
