# Pile de développement locale (étape 0.4)

Fichier : `deploy/compose/dev.yml` (projet `nkulkap-dev`). Toutes les valeurs des fichiers `env/` sont FICTIVES, pour le développement local uniquement.

| Service | Port(s) | Mémoire max |
|---|---|---|
| db (PostgreSQL 18.3) | 5432 | 512 Mo |
| kafka (apache/kafka 4.3.2, KRaft) | 9092 | 768 Mo |
| keycloak (26.8.1, start-dev) | 8180 (admin/admin) | 768 Mo |
| fineract (instance unique, image `fineract-custom:latest`) | 8443 | 1800 Mo |
| toxiproxy (2.13.0) | 8474, 21000-21010 | 64 Mo |

Total : environ 3,9 Go (cible 4,0 Go) hors profil `obs`, pour 5 Go alloués à Docker.

Profil `obs` (non limité en mémoire) : prometheus (9090), grafana (3000, admin/admin), loki (3100).

## Commandes

- `make up` / `make down` / `make logs`
- Avec observabilité : `docker compose -f deploy/compose/dev.yml --profile obs up -d`
- Image Fineract différente : `FINERACT_IMAGE=mon-image:tag make up`

## Test de l'étape 0.4

```
curl -k -H "Fineract-Platform-TenantId: default" -u mifos:password https://localhost:8443/fineract-provider/api/v1/offices
```

Le premier démarrage de Fineract (migrations Liquibase) peut prendre plusieurs minutes.

## Attention

Ne pas lancer de build Gradle de Fineract pendant que la pile tourne sur un poste de 8 Go.
