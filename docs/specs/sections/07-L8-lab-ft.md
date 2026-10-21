<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 563 à 596. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L8 — LAB-FT et fraude (`nkulkap-sentinel`)

**Base réglementaire** : règlement CEMAC relatif à la prévention et à la répression du blanchiment des capitaux et du financement du terrorisme ; déclarations de soupçon à l'ANIF **[À VÉRIFIER : référence du règlement en vigueur, seuils, format de la déclaration]**.

**Exigences**

- **FR-L8-01** — Entrées :
  - événements Fineract (Kafka, Avro) : clients, transactions d'épargne et de prêt ;
  - événements `nkulkap-sync` (avec GPS) ;
  - événements `nkulkap-pay`.
  Les types d'événements externes nécessaires sont activés dans Fineract (`/externalevents/configuration`).
- **FR-L8-02** — Moteur de règles : chaque règle est une fonction pure `(état, événement) → (état', alertes)`. Les seuils sont paramétrables par locataire.
- **FR-L8-03** — Règles v1 :
  1. opération en espèces ≥ seuil réglementaire ;
  2. fractionnement : plusieurs dépôts juste sous le seuil sur N jours ;
  3. vélocité anormale (nombre et montant par jour) ;
  4. entrée puis sortie rapide (> 80 % retiré sous 48 heures) ;
  5. réactivation d'un compte dormant suivie d'un gros mouvement ;
  6. nombreux déposants tiers vers un même compte ;
  7. gros dépôt sur un compte récent.
- **FR-L8-04** — Filtrage des listes de sanctions et des personnes politiquement exposées, à l'entrée en relation et à chaque mise à jour des listes. Correspondance approchée (Jaro-Winkler après normalisation des accents, de l'ordre des noms et des particules). Sources : liste consolidée du Conseil de sécurité de l'ONU (XML public), listes nationales et liste PPE interne importées.
- **FR-L8-05** — Règles propres à la collecte :
  1. collecte hors de la zone habituelle (GPS) ;
  2. lot synchronisé plus de 48 heures après la saisie ;
  3. écarts de tournée répétés ;
  4. taux d'annulation élevé ;
  5. collectes en série à des heures improbables.
- **FR-L8-06** — Dossiers : `NOUVEAU → EN_REVUE → CLASSÉ_SANS_SUITE | ESCALADÉ → DÉCLARÉ`. Tout passe par le journal d'audit, avec commentaires et pièces jointes. Le principe des quatre yeux s'applique avant une déclaration.
- **FR-L8-07** — Correspondance de sanctions confirmée : blocage des débits du compte par l'API Fineract, après validation humaine ou automatiquement selon la configuration du locataire.
- **FR-L8-08** — Export d'un brouillon de déclaration de soupçon pour l'ANIF (format à obtenir) ; en attendant, un PDF structuré.
- **FR-L8-09** — Rejouabilité : le moteur peut rejouer un historique d'événements (depuis un décalage Kafka ou une date) pour tester une nouvelle règle sans créer d'alerte réelle (mode « ombre »).

**Acceptation** : E2E-09 ; chaque règle a des tests par propriétés (ScalaCheck) ; taux de faux positifs mesuré sur le jeu de démonstration et documenté.

