<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1108 à 1140. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 10 — L8 LAB-FT

**Étape 10.1 — Squelette `nkulkap-sentinel`**
- **Faire** : projet sbt, Cats Effect, consommateur fs2-kafka avec décodage Avro (vulcan) des événements Fineract et NKULKAP, PostgreSQL (doobie, Flyway), API http4s, contrôles de santé.
- **Terminé quand** : un événement de dépôt est consommé et persisté en test d'intégration (testcontainers).

**Étape 10.2 — Moteur et règles v1**
- **Faire** : interface `Rule[S]` (fonction pure, FR-L8-02), état par client stocké en base, règles FR-L8-03 et FR-L8-05, seuils par locataire.
- **Tests** : ScalaCheck pour chaque règle (pas d'alerte sous le seuil ; alerte au-dessus ; ordre des événements sans effet pour les règles commutatives).
- **Terminé quand** : les 12 règles sont testées.

**Étape 10.3 — Listes et filtrage**
- **Faire** : import de la liste consolidée de l'ONU (XML), import CSV des listes nationales et PPE, normalisation des noms, correspondance Jaro-Winkler avec seuil configurable, refiltrage complet à chaque mise à jour de liste (FR-L8-04).
- **Terminé quand** : jeu de tests de 200 paires de noms (variantes d'orthographe, inversions prénom/nom) avec rappel ≥ 95 % documenté.

**Étape 10.4 — Dossiers**
- **Faire** : cycle de vie (FR-L8-06), quatre yeux, audit, pièces jointes.
- **Terminé quand** : tests de chaque transition et de chaque refus.

**Étape 10.5 — Actions**
- **Faire** : blocage des débits par l'API Fineract (FR-L8-07), brouillon de déclaration PDF (FR-L8-08).
- **Terminé quand** : E2E-09 passe.

**Étape 10.6 — Mode ombre**
- **Faire** : rejeu depuis une date ou un décalage Kafka dans un espace d'alertes séparé (FR-L8-09).
- **Terminé quand** : une nouvelle règle est évaluée sur 12 mois de données fictives sans alerte réelle.

**Étape 10.7 — Écrans**
- **Faire** : tableau des alertes, dossiers, listes, paramètres des règles.
- **Terminé quand** : parcours validés, accès limité au rôle conformité.

---

