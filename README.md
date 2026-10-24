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

Prérequis : Git, GNU Make, JDK 25 (exigé par Fineract `develop`), Docker.

```sh
make help       # liste des cibles
make bootstrap  # sous-modules, dépendances, liens
make up         # lance l'environnement local
```

## Poste Windows 11 + WSL2

1. Dans PowerShell : `wsl --install -d Ubuntu-24.04`, puis ouvrir Ubuntu et créer l'utilisateur.
2. Limiter la mémoire de WSL dans `%USERPROFILE%\.wslconfig`, puis `wsl --shutdown` :
   ```ini
   [wsl2]
   memory=8GB
   swap=8GB
   ```
3. Installer Docker Desktop et activer Settings > Resources > WSL integration pour Ubuntu-24.04.
4. Dans Ubuntu, installer la chaîne d'outils (git, make, gh, JDK 25, Go, Node 22, Python 3) et cloner le dépôt :
   ```sh
   curl -fsSL --retry 3 -o /tmp/setup-poste-wsl.sh https://raw.githubusercontent.com/nkulkap/nkulkap/main/scripts/setup-poste-wsl.sh
   bash /tmp/setup-poste-wsl.sh --clone
   ```
   Depuis un clone existant : `bash scripts/setup-poste-wsl.sh --clone`. Le script est relançable sans dommage ; `--clone [répertoire]` clone dans `~/nkulkap` par défaut puis lance `make bootstrap`.

Travailler dans le système de fichiers WSL (`~/nkulkap`), pas sous `/mnt/c`.

## Pour aller plus loin

- [Avancement](docs/STATUS.md)
- [Spécifications](docs/specs/README.md)
- [Décisions d'architecture (ADR)](docs/adr/README.md)

## Licence

Logiciel propriétaire, © 2026 Circaura Sarl, tous droits réservés : voir [LICENSE](LICENSE). Les composants tiers (Apache Fineract, applications openMF) restent sous leurs propres licences : voir [NOTICE](NOTICE).
