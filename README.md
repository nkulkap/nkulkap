# NKULKAP

Le nom vient de *Nkul*, le tambour d'appel des Beti, et de *Nkap*, l'argent en camfranglais.

NKULKAP est un core banking pour les EMF et les banques du Cameroun, construit sur Apache Fineract et conçu pour fonctionner sur un réseau peu fiable.

## Dossiers de premier niveau

| Dossier | Contenu |
|---|---|
| `docs/` | Avancement, ADR, spécifications, runbooks, textes réglementaires |
| `core/` | Fork de Fineract (sous-module) et modules custom NKULKAP |
| `services/` | Services satellites : sync, edge, pay, rules, sentinel |
| `apps/` | Applications mobile et web (sous-modules, MPL-2.0) |
| `rules/` | Scripts Numscript et plan de comptes |
| `contracts/` | Contrats d'API et schémas d'événements |
| `reports/` | Gabarits d'états réglementaires |
| `simulators/` | Simulateurs MTN MoMo et Orange Money |
| `deploy/` | Compose, Helm, image du nœud d'agence |
| `testing/` | Profils réseau, fixtures fictives, scénarios E2E |
| `.github/` | Workflows CI |

## Démarrer

Prérequis : Git, GNU Make, JDK 21, Docker.

```sh
make help       # liste des cibles
make bootstrap  # sous-modules, dépendances, liens
make up         # lance l'environnement local
```

## Pour aller plus loin

- [Avancement](docs/STATUS.md)
- [Spécifications](docs/specs/README.md)
- [Décisions d'architecture (ADR)](docs/adr/README.md)
