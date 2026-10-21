<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1182 à 1197. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Phase 13 — Migration et pilote

**Étape 13.1 — Outil de migration**
- **Faire** : gabarits CSV d'import depuis l'ancien logiciel de l'EMF (clients, comptes, soldes d'ouverture, prêts en cours avec échéanciers, carnets, tontines en cours). Validateur, import par lots idempotent, rapport de rapprochement des soldes ancien / nouveau, au franc près.
- **Terminé quand** : migration à blanc d'un jeu fictif de 20 000 clients, avec écart de solde total = 0.

**Étape 13.2 — Pilote**
- **Faire** : une EMF pilote, 1 ou 2 agences dont une rurale, 5 collecteurs. Deux à quatre semaines de fonctionnement en parallèle de l'ancien système. Critères de bascule :
  - 0 doublon ;
  - 0 perte ;
  - écarts de caisse expliqués à 100 % ;
  - états du mois identiques entre les deux systèmes.
- **Terminé quand** : décision formelle de bascule (go / no-go) signée par l'EMF.

---

