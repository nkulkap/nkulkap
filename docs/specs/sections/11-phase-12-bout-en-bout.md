<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1150 à 1181. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 12 — Bout en bout et industrialisation

**Étape 12.1 — Suite E2E complète**
- **Faire** : E2E-01 à E2E-12 automatisés dans la CI nocturne, avec rapport HTML.
- **Terminé quand** : 10 nuits consécutives vertes.

**Étape 12.2 — Livraison**
- **Faire** :
  - `deploy/compose/emf.yml` : mono-EMF, avec script d'installation interactif en FR et EN ;
  - `deploy/helm` : SaaS ;
  - `deploy/edge` : image du nœud d'agence.
  Images signées. Notes de version FR et EN.
- **Terminé quand** : installation de bout en bout sur un serveur vierge en moins de 2 heures, en suivant uniquement le guide.

**Étape 12.3 — Charge et dimensionnement**
- **Faire** : scénarios k6 (guichet, synchronisation de 50 collecteurs, fin de journée, fin de mois) sur le jeu `large`. Mettre à jour NFR-12 et NFR-13 avec les mesures réelles.
- **Terminé quand** : rapport de dimensionnement publié dans `docs/`.

**Étape 12.4 — Sauvegarde et restauration**
- **Faire** : pgBackRest, archivage WAL, copie hors site, test de restauration mensuel automatisé (NFR-10), runbooks d'incident (perte de serveur, perte de nœud d'agence, téléphone volé, opérateur MoMo indisponible).
- **Terminé quand** : restauration chronométrée sous le RTO.

**Étape 12.5 — Sécurité**
- **Faire** : durcissement (en-têtes, CSP du web, ports, utilisateurs non privilégiés dans les conteneurs), revue des rôles, analyse des dépendances, test d'intrusion externe.
- **Terminé quand** : aucune vulnérabilité critique ou haute ouverte.

**Étape 12.6 — Documentation**
- **Faire** : guides utilisateurs par rôle (FR et EN), guide administrateur, guide d'exploitation, référence des API.
- **Terminé quand** : documentation relue par un utilisateur pilote.

---

