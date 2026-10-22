#!/usr/bin/env bash
# Exporte le royaume Keycloak nkulkap vers deploy/keycloak/realm-nkulkap.json.
# Licence : voir LICENSE à la racine du dépôt.
#
# L'export à chaud échoue (base H2 verrouillée) : on arrête Keycloak, on copie sa base,
# on lance un conteneur d'export éphémère sur la copie, puis on redémarre Keycloak.
# Fonctionne sous Git Bash (cygpath, pwd -W) et sous Linux (chemins tels quels).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

COMPOSE_FILE="deploy/compose/dev.yml"
CONTAINER="${KEYCLOAK_CONTAINER:-nkulkap-dev-keycloak-1}"
IMAGE="${KEYCLOAK_IMAGE:-quay.io/keycloak/keycloak:26.8.1}"
REALM="nkulkap"
OUT_FILE="realm-nkulkap.json"

# Empêche Git Bash de convertir les chemins Linux passés à docker.
export MSYS_NO_PATHCONV=1

# Conversion en chemin Windows seulement si cygpath existe (Git Bash) ; sinon chemin tel quel.
winpath() { if command -v cygpath >/dev/null 2>&1; then cygpath -w "$1"; else printf '%s' "$1"; fi; }
if pwd -W >/dev/null 2>&1; then OUT_DIR="$(pwd -W)/deploy/keycloak"; else OUT_DIR="$ROOT/deploy/keycloak"; fi

TMP="$(mktemp -d)"
STOPPED=0

# Redémarre Keycloak et nettoie, y compris en cas d'échec d'une étape.
cleanup() {
  status=$?
  if [ "$STOPPED" = "1" ]; then
    echo "Redémarrage de Keycloak..."
    docker compose -f "$COMPOSE_FILE" start keycloak || echo "Échec du redémarrage de Keycloak." >&2
  fi
  rm -rf "$TMP"
  exit "$status"
}
trap cleanup EXIT

echo "1/4 Arrêt de Keycloak"
docker compose -f "$COMPOSE_FILE" stop keycloak
STOPPED=1

echo "2/4 Copie de la base H2"
docker cp "$CONTAINER:/opt/keycloak/data/h2" "$(winpath "$TMP")"

echo "3/4 Export du royaume $REALM"
docker run --rm \
  -v "$(winpath "$TMP/h2"):/opt/keycloak/data/h2" \
  -v "$OUT_DIR:/out" \
  "$IMAGE" export --file "/out/$OUT_FILE" --realm "$REALM" --users same_file

# Formatage stable pour des diffs lisibles (sans dépendance externe).
PY="$(python3 -c '' >/dev/null 2>&1 && echo python3 || echo python)"
"$PY" -I -c '
import json, sys
p = sys.argv[1]
with open(p, encoding="utf-8") as f:
    d = json.load(f)
with open(p, "w", encoding="utf-8", newline="\n") as f:
    json.dump(d, f, indent=2, ensure_ascii=False, sort_keys=False)
    f.write("\n")
' "$ROOT/deploy/keycloak/$OUT_FILE"

echo "4/4 Terminé : deploy/keycloak/$OUT_FILE"
