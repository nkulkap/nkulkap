#!/usr/bin/env bash
# Installe la chaîne d'outils NKULKAP sur un poste Ubuntu 24.04 (WSL2 sous Windows 11).
# Idempotent : relançable sans dommage. Docker n'est pas installé ici : Docker Desktop
# le fournit dans WSL (intégration WSL activée), le script vérifie seulement sa présence.
#
# Usage : bash scripts/setup-poste-wsl.sh [--clone [répertoire]]
#   --clone  clone le dépôt (défaut ~/nkulkap) avec ses sous-modules, puis lance make bootstrap.
set -euo pipefail

GO_VERSION="1.27.0"
JAVA_MAJOR="25"
NODE_MAJOR="22"
NODE_MIN="22.12"
PYTHON_MIN="3.12"
REPO_URL="https://github.com/nkulkap/nkulkap.git"
KEYRINGS="/etc/apt/keyrings"
CURL=(curl -fsSL --retry 3 --connect-timeout 15)

CLONE=0
CLONE_DIR="$HOME/nkulkap"
while [ $# -gt 0 ]; do
  case "$1" in
    --clone)
      CLONE=1
      if [ $# -gt 1 ] && [ "${2#--}" = "$2" ]; then CLONE_DIR="$2"; shift; fi
      ;;
    -h|--help) sed -n '2,7p' "$0"; exit 0 ;;
    *) echo "Option inconnue : $1" >&2; exit 2 ;;
  esac
  shift
done

info() { printf '\n==> %s\n' "$*"; }
warn() { printf 'AVERTISSEMENT : %s\n' "$*" >&2; }
die()  { printf 'ERREUR : %s\n' "$*" >&2; exit 1; }

# sudo seulement là où il faut (inutile si on est déjà root)
if [ "$(id -u)" -eq 0 ]; then
  SUDO=()
else
  command -v sudo >/dev/null 2>&1 || die "sudo est requis (ou lancer en root)."
  SUDO=(sudo)
fi

if [ -r /etc/os-release ]; then
  # shellcheck disable=SC1091
  . /etc/os-release
fi
if [ "${ID:-}" != "ubuntu" ] || [ "${VERSION_ID:-}" != "24.04" ]; then
  warn "prévu pour Ubuntu 24.04 (détecté : ${PRETTY_NAME:-inconnu}) ; la suite peut échouer."
fi
ARCH="$(dpkg --print-architecture)"
[ "$ARCH" = "amd64" ] || die "seule l'architecture amd64 est prise en charge (détecté : $ARCH)."

# version_ge A B : vrai si A >= B (comparaison de versions)
version_ge() { [ "$(printf '%s\n%s\n' "$2" "$1" | sort -V | head -n1)" = "$2" ]; }

# Télécharge une clé de dépôt apt dans /etc/apt/keyrings (armorée .asc ou binaire .gpg, les deux sont acceptées par apt)
installer_cle() { # url fichier
  local tmp
  tmp="$(mktemp)"
  "${CURL[@]}" -o "$tmp" "$1" || { rm -f "$tmp"; die "téléchargement impossible : $1"; }
  "${SUDO[@]}" install -d -m 0755 "$KEYRINGS"
  "${SUDO[@]}" install -m 0644 "$tmp" "$2"
  rm -f "$tmp"
}

# Écrit un fichier de source apt s'il diffère (idempotent)
ecrire_source() { # fichier contenu
  if [ ! -f "$1" ] || [ "$(cat "$1")" != "$2" ]; then
    printf '%s\n' "$2" | "${SUDO[@]}" tee "$1" >/dev/null
  fi
}

# ---------------------------------------------------------------- 1. paquets de base
info "1/6 Paquets de base (apt)"
"${SUDO[@]}" apt-get update -qq
"${SUDO[@]}" env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq \
  git make curl ca-certificates unzip zip jq build-essential python3 python3-venv >/dev/null
PY_VER="$(python3 -c 'import sys; print("%d.%d" % sys.version_info[:2])')"
version_ge "$PY_VER" "$PYTHON_MIN" || die "python3 $PY_VER trouvé, $PYTHON_MIN minimum requis."

# ---------------------------------------------------------------- 2. GitHub CLI
info "2/6 GitHub CLI (dépôt apt officiel)"
installer_cle "https://cli.github.com/packages/githubcli-archive-keyring.gpg" "$KEYRINGS/githubcli-archive-keyring.gpg"
ecrire_source /etc/apt/sources.list.d/github-cli.list \
  "deb [arch=$ARCH signed-by=$KEYRINGS/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main"

# ---------------------------------------------------------------- 3. JDK 25
info "3/6 JDK $JAVA_MAJOR"
java_major() { java -version 2>&1 | sed -n '1s/.*version "\([0-9]*\).*/\1/p'; }
candidat() { apt-cache policy "$1" 2>/dev/null | sed -n 's/^ *Candidate: //p' | grep -v '(none)' || true; }
JAVA_PKG=""
if [ "$(java_major 2>/dev/null || true)" != "$JAVA_MAJOR" ]; then
  installer_cle "https://packages.microsoft.com/keys/microsoft.asc" "$KEYRINGS/microsoft.asc"
  ecrire_source /etc/apt/sources.list.d/microsoft-prod.list \
    "deb [arch=$ARCH signed-by=$KEYRINGS/microsoft.asc] https://packages.microsoft.com/ubuntu/24.04/prod noble main"
  "${SUDO[@]}" apt-get update -qq || warn "mise à jour apt partielle (dépôt Microsoft)."
  if [ -n "$(candidat "msopenjdk-$JAVA_MAJOR")" ]; then
    JAVA_PKG="msopenjdk-$JAVA_MAJOR"
  else
    warn "msopenjdk-$JAVA_MAJOR introuvable : repli sur Eclipse Temurin $JAVA_MAJOR (Adoptium)."
    installer_cle "https://packages.adoptium.net/artifactory/api/gpg/key/public" "$KEYRINGS/adoptium.asc"
    ecrire_source /etc/apt/sources.list.d/adoptium.list \
      "deb [arch=$ARCH signed-by=$KEYRINGS/adoptium.asc] https://packages.adoptium.net/artifactory/deb noble main"
    "${SUDO[@]}" apt-get update -qq
    JAVA_PKG="temurin-$JAVA_MAJOR-jdk"
    [ -n "$(candidat "$JAVA_PKG")" ] || die "ni msopenjdk-$JAVA_MAJOR ni $JAVA_PKG disponibles."
  fi
fi

# ---------------------------------------------------------------- 4. Node.js 22
info "4/6 Node.js $NODE_MAJOR (NodeSource)"
node_ok() {
  command -v node >/dev/null 2>&1 || return 1
  local v
  v="$(node -v | tr -d v)"
  version_ge "$v" "$NODE_MIN" && [ "${v%%.*}" = "$NODE_MAJOR" ]
}
NODE_PKG=0
if ! node_ok; then
  installer_cle "https://deb.nodesource.com/gpgkey/nodesource-repo.gpg.key" "$KEYRINGS/nodesource.asc"
  ecrire_source /etc/apt/sources.list.d/nodesource.list \
    "deb [arch=$ARCH signed-by=$KEYRINGS/nodesource.asc] https://deb.nodesource.com/node_${NODE_MAJOR}.x nodistro main"
  NODE_PKG=1
fi

# Un seul apt update puis installation de gh, du JDK et de Node
"${SUDO[@]}" apt-get update -qq
PKGS=(gh)
[ -n "$JAVA_PKG" ] && PKGS+=("$JAVA_PKG")
[ "$NODE_PKG" -eq 1 ] && PKGS+=(nodejs)
"${SUDO[@]}" env DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "${PKGS[@]}" >/dev/null
[ "$(java_major || true)" = "$JAVA_MAJOR" ] || die "java -version n'indique pas le JDK $JAVA_MAJOR."
node_ok || die "Node.js >= $NODE_MIN (branche $NODE_MAJOR) attendu."

# ---------------------------------------------------------------- 5. Go
info "5/6 Go $GO_VERSION"
go_ok() { [ -x /usr/local/go/bin/go ] && [ "$(/usr/local/go/bin/go version | awk '{print $3}')" = "go$GO_VERSION" ]; }
if ! go_ok; then
  TARBALL="go${GO_VERSION}.linux-amd64.tar.gz"
  TMPD="$(mktemp -d)"
  trap 'rm -rf "$TMPD"' EXIT
  "${CURL[@]}" -o "$TMPD/dl.json" "https://go.dev/dl/?mode=json&include=all" || die "liste des versions Go inaccessible."
  SHA="$(jq -r --arg v "go$GO_VERSION" --arg f "$TARBALL" \
        '.[] | select(.version==$v) | .files[] | select(.filename==$f) | .sha256' "$TMPD/dl.json")"
  [ -n "$SHA" ] || die "somme SHA-256 de $TARBALL introuvable sur go.dev/dl."
  "${CURL[@]}" -o "$TMPD/$TARBALL" "https://go.dev/dl/$TARBALL" || die "téléchargement impossible : $TARBALL"
  echo "$SHA  $TMPD/$TARBALL" | sha256sum -c --status || die "somme SHA-256 invalide pour $TARBALL."
  "${SUDO[@]}" rm -rf /usr/local/go
  "${SUDO[@]}" tar -C /usr/local -xzf "$TMPD/$TARBALL"
fi
# Le PATH est fixé dans un fichier unique, réécrit à l'identique à chaque exécution
# shellcheck disable=SC2016 # $PATH doit rester littéral dans le fichier écrit
printf '%s\n' 'export PATH="$PATH:/usr/local/go/bin"' | "${SUDO[@]}" tee /etc/profile.d/nkulkap-go.sh >/dev/null
export PATH="$PATH:/usr/local/go/bin"
go_ok || die "Go $GO_VERSION non fonctionnel après installation."

# ---------------------------------------------------------------- 6. Réglages
info "6/6 Réglages Git et Gradle"
# LF à l'extraction, conversion CRLF -> LF à l'ajout. core.longpaths est inutile sous Linux.
git config --global core.autocrlf input
GRADLE_PROPS="$HOME/.gradle/gradle.properties"
if [ ! -f "$GRADLE_PROPS" ]; then
  mkdir -p "$HOME/.gradle"
  cat > "$GRADLE_PROPS" <<'PROPS'
# Le -Xmx12g de Fineract est trop pour la mémoire donnée à WSL par défaut.
# Ajuster selon la mémoire allouée à WSL (clé memory de .wslconfig).
org.gradle.jvmargs=-Xmx6g
PROPS
  echo "Créé : $GRADLE_PROPS"
else
  echo "Conservé tel quel : $GRADLE_PROPS existe déjà."
fi

# ---------------------------------------------------------------- Clonage (option)
if [ "$CLONE" -eq 1 ]; then
  info "Clonage du dépôt dans $CLONE_DIR"
  if [ -d "$CLONE_DIR/.git" ]; then
    echo "Déjà cloné : $CLONE_DIR"
  else
    git clone --recurse-submodules "$REPO_URL" "$CLONE_DIR"
  fi
  make -C "$CLONE_DIR" bootstrap
fi

# ---------------------------------------------------------------- Vérification
info "Vérification"
MANQUE=0
ligne() { # nom obligatoire(1/0) commande [arguments...]
  local nom="$1" oblig="$2" v
  shift 2
  if command -v "$1" >/dev/null 2>&1 && v="$("$@" 2>&1 | head -n1)" && [ -n "$v" ]; then
    printf '  %-16s %s\n' "$nom" "$v"
  else
    printf '  %-16s %s\n' "$nom" "ABSENT"
    if [ "$oblig" -eq 1 ]; then MANQUE=1; fi
  fi
  return 0
}
ligne git 1 git --version
ligne make 1 make --version
ligne gh 1 gh --version
ligne java 1 java -version
ligne go 1 go version
ligne node 1 node --version
ligne npm 1 npm --version
ligne python3 1 python3 --version
ligne docker 0 docker --version
ligne "docker compose" 0 docker compose version
if ! command -v docker >/dev/null 2>&1; then
  warn "docker est absent : activer Docker Desktop > Settings > Resources > WSL integration pour Ubuntu-24.04."
fi
if [ "$MANQUE" -ne 0 ]; then die "au moins un outil obligatoire est absent."; fi
echo "Poste prêt."
