<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 406 à 423. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### L3 — Commissions et répartitions en Numscript (`nkulkap-rules`)

**Ce que Fineract fournit déjà** : 15 déclencheurs de frais, 5 modes de calcul, taxes sur frais et sur intérêts d'épargne. NKULKAP **ne réécrit pas** ces frais.

**Ce que Numscript ajoute** : la répartition d'un montant entre plusieurs bénéficiaires (agent, collecteur, opérateur, État, EMF), avec des arrondis XAF déterministes.

**Exigences**

- **FR-L3-01** — `nkulkap-rules` expose `POST /v1/evaluate` avec `{tenant, rule_id, rule_version, variables}` et renvoie `{postings:[{source, destination, amount, asset}], rule_sha256}`.
- **FR-L3-02** — Les règles sont des fichiers `rules/<domaine>/<nom>.num`. Chacune est versionnée (version sémantique dans l'en-tête de commentaire) et accompagnée de tests.
- **FR-L3-03** — `rules/account_map.yaml` traduit chaque compte Numscript (`emf:produits:commissions_collecte`) en code GL Fineract, par locataire et éventuellement par agence.
- **FR-L3-04** — L'appelant (module Fineract ou `nkulkap-pay`) transforme les écritures en **une** écriture comptable équilibrée Fineract (`POST /journalentries`), avec `referenceNumber` = `op_id`.
- **FR-L3-05** — Les scripts sont purs : la source est toujours `allowing unbounded overdraft`, aucun solde n'est lu.
- **FR-L3-06** — Règles livrées en v1 : frais de collecte (annexe B.1), frais de retrait mobile money avec TVA et part d'agent (annexe B.2), commission de tontine.
- **FR-L3-07** — Les barèmes par tranches (grilles de frais) se calculent avant Numscript, dans l'appelant, à partir d'une table `nkulkap_fee_grid`. Numscript reçoit le montant final des frais.

**Acceptation** : chaque règle a au moins 5 cas de test, dont les cas limites (montant 0 refusé, montant 1 XAF, montants très élevés, parts à 0 % et 100 %) ; la somme des écritures est toujours égale au montant d'entrée.

