# Harnais de réseau dégradé (Toxiproxy)

Étape 2.3. Profils de la section 10.2 dans `profiles.yaml`, appliqués au serveur Toxiproxy de la pile (`deploy/compose/dev.yml`, API sur `localhost:8474`).

| Mandataire | Écoute (poste) | Relaie vers |
|---|---|---|
| `fineract` | `localhost:21000` | `fineract:8443` (HTTPS) |
| `kafka` | `localhost:21001` | `kafka:9092` |

Un client de test passe par l'adresse d'écoute au lieu de l'adresse directe (ex. `https://localhost:21000/fineract-provider/api/v1`).

## Commande `toxictl`

```bash
make toxiproxy PROFILE=edge_rural
```

| Commande | Effet |
|---|---|
| `toxictl init` | crée ou met à jour les mandataires |
| `toxictl apply <profil> [mandataire]` | remplace les toxiques par ceux du profil (mandataire `fineract` par défaut) |
| `toxictl reset` | retire tous les toxiques |
| `toxictl list` | liste les profils |

Variables : `TOXIPROXY_URL` (défaut `http://localhost:8474`), `TOXIPROXY_PROFILES` (défaut : le `profiles.yaml` de ce dossier). Code de sortie non nul en cas d'échec : un test qui l'appelle doit s'arrêter, pas continuer en réseau normal. Binaire : `go build -o toxictl ./cmd/toxictl`.

## Depuis chaque langage

La bascule passe par la commande (ou directement par l'API HTTP de Toxiproxy, `POST /proxies/{nom}/toxics`, `POST /reset`) :

| Langage | Appel |
|---|---|
| Rust | `std::process::Command::new("toxictl").args(["apply", "edge_rural"]).status()` |
| Java | `new ProcessBuilder("toxictl", "apply", "edge_rural").inheritIO().start().waitFor()` |
| Scala | `sys.process.Process(Seq("toxictl", "apply", "edge_rural")).!` |
| Go | paquet `github.com/nkulkap/nkulkap/testing/toxiproxy` (`LoadProfiles`, `Client.Apply`) |
| Scripts E2E | `toxictl apply coupure_longue` … `toxictl reset` |

Chaque test remet le réseau normal à la fin (`toxictl reset`), y compris en cas d'échec.

## Mode `reponse_perdue`

Toxique `limit_data` à 0 octet sur le flux de retour : la requête part en entier, Fineract la traite, la connexion se ferme avant le premier octet de réponse. Fineract étant en HTTPS, le toxique doit être ajouté **après** la poignée de main TLS (Toxiproxy l'applique aux connexions déjà ouvertes) ; sinon la poignée de main elle-même échoue et la requête ne part jamais.

`reponse_perdue_test.go` le démontre (`make toxiproxy-test`, pile et seed requis) : dépôt envoyé à travers le mandataire, aucun octet reçu (EOF), dépôt présent côté serveur (relecture par `externalId`), rejeu avec la même `Idempotency-Key` servi depuis le cache, sans doublon.

## Limites

- La « perte de paquets » est simulée par des coupures de connexion (`reset_peer`) sur une fraction des connexions (`toxicity`), comme le prévoit la section 10.2.
- `coupure_longue` coupe le lien sans limite de durée : le scénario E2E gère le temps (72 h simulées) et retire le profil.
- Débits en Ko/s (unité de Toxiproxy) : 40 kbit/s = 5 Ko/s, 1 Mbit/s = 125 Ko/s, 512 kbit/s = 64 Ko/s.
