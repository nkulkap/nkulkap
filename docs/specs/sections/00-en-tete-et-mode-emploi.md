<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1 à 68. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

# NKULKAP — Cahier des charges et plan d'implémentation

> **NKULKAP** : un core banking pour les EMF et les banques du Cameroun. Il est construit sur Apache Fineract et conçu pour un terrain où le réseau, l'électricité et les téléphones ne sont pas fiables.
>
> **Origine du nom.** *Nkul* est le tambour d'appel des Beti, leur moyen traditionnel de télécommunication : un message passe de village en village sans aucun réseau moderne. *Nkap* désigne l'argent en camfranglais, la langue partagée par tous les Camerounais. **Nkulkap** signifie donc « le tam-tam de l'argent » : un système qui fait circuler l'argent et ses messages de façon sûre, même quand le réseau ne suit pas. C'est exactement la promesse technique du produit.

| Élément | Valeur |
|---|---|
| Version du document | 1.0 |
| Date | 21 octobre 2026 |
| Base de code de référence | Apache Fineract, branche `develop`, commit `b94b2ddf` du 20 octobre 2026 (postérieur à la 1.15.0) |
| Applications de référence | `openMF/web-app` (Angular), `openMF/android-client` (Kotlin Multiplatform), commit du 15 octobre 2026 |
| Langue du produit | Français et anglais (bilinguisme officiel du Cameroun) |
| Statut | Référence de travail. Les points marqués **[À VÉRIFIER]** reposent sur une source secondaire ou sur une hypothèse, et doivent être confirmés avant la mise en production. |

> Une recherche web du 21 octobre 2026 n'a trouvé aucun usage existant de « Nkulap », l'ancien nom du projet. **[À VÉRIFIER]** : refaire cette recherche pour « Nkulkap », puis vérifier la disponibilité officielle (marque à l'OAPI, noms de domaine) avant tout usage commercial.

---

## Table des matières

0. [Mode d'emploi pour Claude Code et les ingénieurs](#0-mode-demploi-pour-claude-code-et-les-ingénieurs)
1. [Vision et périmètre](#1-vision-et-périmètre)
2. [Contraintes du terrain camerounais](#2-contraintes-du-terrain-camerounais)
3. [Décisions d'architecture (ADR)](#3-décisions-darchitecture-adr)
4. [Architecture cible](#4-architecture-cible)
5. [Stack technologique complet](#5-stack-technologique-complet)
6. [Organisation des dépôts](#6-organisation-des-dépôts)
7. [Exigences fonctionnelles par ligne](#7-exigences-fonctionnelles-par-ligne)
8. [Exigences non fonctionnelles](#8-exigences-non-fonctionnelles)
9. [Sécurité, conformité, données personnelles et licences](#9-sécurité-conformité-données-personnelles-et-licences)
10. [Stratégie de tests](#10-stratégie-de-tests)
11. [Plan d'implémentation pas à pas](#11-plan-dimplémentation-pas-à-pas)
12. [Entrées humaines requises](#12-entrées-humaines-requises)
13. [Risques et points ouverts](#13-risques-et-points-ouverts)
14. [Annexes](#14-annexes)

---

## 0. Mode d'emploi pour Claude Code et les ingénieurs

Ce document se suit dans l'ordre. La section 11 est la feuille de route exécutable. Les sections 1 à 10 sont la référence que chaque étape cite.

### 0.1 Règles d'or (non négociables)

1. **Fineract est la seule source de vérité comptable.** Aucun autre service ne tient de solde officiel. Les services satellites postent dans Fineract par son API, avec une clé d'idempotence.
2. **Toute opération financière porte un `op_id` (UUIDv7) créé au moment de la saisie.** Il est persisté avant tout appel réseau et réutilisé à chaque nouvelle tentative. On ne génère jamais un nouvel identifiant pour retenter.
3. **Les montants XAF sont des entiers.** Le franc CFA BEAC a 0 décimale. Ne jamais utiliser `float` ni `double` pour l'argent : `long` ou `i64` pour stocker, `BigDecimal` pour calculer, avec un mode d'arrondi explicite.
4. **Ne pas modifier le cœur de Fineract.** Tout le code NKULKAP côté Fineract vit dans `custom/nkulkap/**`. Toute exception passe par un ADR approuvé et se propose en contribution amont.
5. **Chaque appel réseau a un délai d'expiration, une politique de nouvelle tentative et un comportement défini en cas d'échec.** Pas d'appel « fire and forget ».
6. **Tests d'abord pour tout calcul financier** : TEG, commissions, répartitions, intérêts, rapprochements.
7. **Aucune chaîne d'interface en dur.** Tout passe par des clés i18n, fournies en `fr` et en `en`.
8. **Les données réglementaires manquantes ne s'inventent pas.** Si une étape a besoin d'une entrée humaine (section 12), utiliser le jeu de données de remplacement marqué `FICTIF`, le signaler dans `docs/STATUS.md`, puis continuer.
9. **Fin d'étape** : tests verts, `docs/STATUS.md` mis à jour, puis commit au format Conventional Commits.

### 0.2 Comment exécuter une étape

Chaque étape de la section 11 suit le même format :

- **Faire** : ce qu'il faut construire.
- **Fichiers** : où le construire.
- **Vérifier** : les commandes à lancer.
- **Terminé quand** : les critères d'acceptation, tous obligatoires.

En début de session, Claude Code lit `CLAUDE.md` (gabarit en annexe A), `docs/STATUS.md` et l'étape en cours. Il ne relit les autres sections que lorsque l'étape les cite.

---

