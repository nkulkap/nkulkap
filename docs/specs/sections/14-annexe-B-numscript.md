<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1282 à 1382. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Annexe B — Règles Numscript validées

Les deux scripts ci-dessous ont été exécutés avec la bibliothèque Numscript (version v0.0.24, licence MIT). Les sorties reproduites sont celles obtenues. Les taux sont des **exemples** à remplacer par les valeurs de l'EMF et de l'expert fiscal (I-06, I-13).

#### B.1 Frais de collecte : part du collecteur

`rules/collecte/frais_collecte.num`

```numscript
// rule: collecte/frais_collecte  version: 1.0.0
// Répartit les frais de collecte d'un cycle entre le collecteur et l'EMF.
vars {
  monetary $frais
  portion $part_collecteur
  account $collecteur
}

send $frais (
  source = @emf:frais_a_repartir allowing unbounded overdraft
  destination = {
    $part_collecteur to $collecteur
    remaining to @emf:produits:commissions_collecte
  }
)
```

Entrée :

```json
{"variables": {"frais": "XAF 3100", "part_collecteur": "40%", "collecteur": "collecteur:42"}}
```

Sortie obtenue :

```json
[
  {"source": "emf:frais_a_repartir", "destination": "collecteur:42", "amount": 1240, "asset": "XAF"},
  {"source": "emf:frais_a_repartir", "destination": "emf:produits:commissions_collecte", "amount": 1860, "asset": "XAF"}
]
```

#### B.2 Frais de retrait mobile money : TVA, agent et EMF

`rules/momo/frais_retrait.num`. La part de TVA dans un montant TTC à 19,25 % vaut 19,25 / 119,25, soit la fraction exacte `77/477`.

```numscript
// rule: momo/frais_retrait  version: 1.0.0
vars {
  monetary $frais_ttc
  portion $taux_tva
  portion $part_agent
  account $agent
}

send $frais_ttc (
  source = @emf:frais_a_repartir allowing unbounded overdraft
  destination = {
    $taux_tva to @etat:tva_collectee
    remaining to {
      $part_agent to $agent
      remaining to @emf:produits:commissions_momo
    }
  }
)
```

Entrée :

```json
{"variables": {"frais_ttc": "XAF 1000", "taux_tva": "77/477", "part_agent": "30%", "agent": "agent:7"}}
```

Sortie obtenue :

```json
[
  {"source": "emf:frais_a_repartir", "destination": "etat:tva_collectee", "amount": 162, "asset": "XAF"},
  {"source": "emf:frais_a_repartir", "destination": "agent:7", "amount": 252, "asset": "XAF"},
  {"source": "emf:frais_a_repartir", "destination": "emf:produits:commissions_momo", "amount": 586, "asset": "XAF"}
]
```

> Arrondis : la valeur exacte de la TVA est 161,4 XAF ; Numscript attribue 162 XAF, car il distribue les restes de façon déterministe. La règle d'arrondi fiscale doit être validée par l'expert (I-06). Si elle diffère, calculer la TVA hors Numscript et la passer en montant fixe (`[XAF 161] to @etat:tva_collectee`).

#### B.3 Correspondance des comptes (`rules/account_map.yaml`)

```yaml
tenant: default
version: 1
accounts:
  "emf:frais_a_repartir":            { gl: "FICTIF-471100" }   # compte de liaison
  "emf:produits:commissions_collecte": { gl: "FICTIF-706100" }
  "emf:produits:commissions_momo":    { gl: "FICTIF-706200" }
  "etat:tva_collectee":               { gl: "FICTIF-443100" }
patterns:
  "collecteur:{staffId}":             { gl: "FICTIF-422100", sub_ledger: staff }   # dette envers le collecteur
  "agent:{agentId}":                  { gl: "FICTIF-401500", sub_ledger: agent }
```

Les codes GL sont FICTIFS jusqu'à l'obtention de l'entrée I-01.

