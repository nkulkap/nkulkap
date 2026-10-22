# NKULKAP — commandes du monodépôt. `make help` liste les cibles.
# Sous Windows, lancer depuis Git Bash avec GNU Make installé.

SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := help

FINERACT_DIR   := core/fineract
CUSTOM_SRC     := core/custom-nkulkap
CUSTOM_LINK    := $(FINERACT_DIR)/custom/nkulkap
COMPOSE        := docker compose -f deploy/compose/dev.yml
# link : lien symbolique ; junction : jonction NTFS (Windows) ; copy : copie (dernier recours). Voir ADR-013.
ifneq (,$(findstring MINGW,$(shell uname -s))$(findstring MSYS,$(shell uname -s)))
LINK_MODE      ?= junction
else
LINK_MODE      ?= link
endif
COMPONENT      ?=
SCENARIO       ?=
SPIKE          ?=
PROFILE        ?=
SIZE           ?= small
# python3 sous Linux ; sous Windows, python3 peut n'être que l'alias du Store : repli sur python.
PYTHON         ?= $(shell python3 -c '' >/dev/null 2>&1 && echo python3 || echo python)
GRADLE_FLAGS   ?=
# Modules Fineract NKULKAP : chaque core/custom-nkulkap/<domaine>/<module>/build.gradle -> :custom:nkulkap:<domaine>:<module>
NKULKAP_MODULES := $(subst /,:,$(patsubst $(CUSTOM_SRC)/%/build.gradle,:custom:nkulkap:%,$(wildcard $(CUSTOM_SRC)/*/*/build.gradle)))

.PHONY: help bootstrap build test e2e up down logs seed seed-test spike-test lint-contracts fixtures fixtures-load fixtures-test toxiproxy toxiproxy-test

help: ## Liste les cibles disponibles
	@grep -E '^[a-z0-9-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

bootstrap: ## Initialise les sous-modules et relie les modules custom à Fineract
	git -c http.version=HTTP/1.1 -c core.longpaths=true submodule update --init --recursive
	@if [ ! -d "$(FINERACT_DIR)/custom" ]; then echo "Sous-module $(FINERACT_DIR) absent : voir docs/STATUS.md, étape 0.2." >&2; exit 1; fi
	@# Fineract attend des fins de ligne LF : sans ceci, Git pour Windows extrait en CRLF et le build réécrit les sources.
	@if [ "$$(git -C $(FINERACT_DIR) config core.autocrlf)" != "false" ]; then \
		git -C $(FINERACT_DIR) config core.autocrlf false && git -C $(FINERACT_DIR) config core.eol lf && git -C $(FINERACT_DIR) config core.longpaths true && \
		git -C $(FINERACT_DIR) rm -rq --cached . && git -C $(FINERACT_DIR) reset -q --hard && echo "Sous-module $(FINERACT_DIR) réextrait en LF"; fi
	@if [ "$(LINK_MODE)" = "copy" ]; then \
		rm -rf "$(CUSTOM_LINK)" && mkdir -p "$(CUSTOM_LINK)" && cp -R "$(CUSTOM_SRC)/." "$(CUSTOM_LINK)/" && echo "Copie synchronisée : $(CUSTOM_LINK)"; \
	elif [ -e "$(CUSTOM_LINK)" ]; then echo "Lien déjà présent : $(CUSTOM_LINK)"; \
	elif [ "$(LINK_MODE)" = "junction" ]; then \
		cmd //c "mklink /J $$(cygpath -w "$(CUSTOM_LINK)") $$(cygpath -w "$(CUSTOM_SRC)")" >/dev/null && echo "Jonction créée : $(CUSTOM_LINK) -> $(CUSTOM_SRC)"; \
	else ln -s ../../custom-nkulkap "$(CUSTOM_LINK)" && echo "Lien créé : $(CUSTOM_LINK) -> ../../custom-nkulkap"; fi
	@EXCL="$$(git -C $(FINERACT_DIR) rev-parse --path-format=absolute --git-path info/exclude)"; \
		grep -qx '/custom/nkulkap' "$$EXCL" 2>/dev/null || echo '/custom/nkulkap' >> "$$EXCL"

build: ## Compile les modules Fineract NKULKAP
	cd $(FINERACT_DIR) && ./gradlew $(GRADLE_FLAGS) $(addsuffix :build,$(NKULKAP_MODULES))

test: ## Lance les tests d'un composant : make test COMPONENT=<core|sync|edge|pay|rules|sentinel|field|web>
	@case "$(COMPONENT)" in \
		core) cd $(FINERACT_DIR) && ./gradlew $(GRADLE_FLAGS) $(addsuffix :test,$(NKULKAP_MODULES)) ;; \
		rules) cd services/nkulkap-rules && go vet ./... && go test ./... ;; \
		field) echo "::notice::COMPONENT=field : aucun test exécuté, compilation de apps/nkulkap-field reportée au poste puissant (étape 1.6 partielle, docs/STATUS.md)." ;; \
		sync|edge|pay|sentinel|web) echo "COMPONENT=$(COMPONENT) : pas encore créé (voir docs/STATUS.md)." >&2; exit 1 ;; \
		*) echo "Préciser COMPONENT=<core|sync|edge|pay|rules|sentinel|field|web>." >&2; exit 2 ;; \
	esac

e2e: ## Lance un scénario de bout en bout : make e2e SCENARIO=E2E-01
	@echo "Scénarios E2E pas encore écrits (phase 12)." >&2; exit 1

up: ## Démarre la pile de développement (Docker Compose)
	$(COMPOSE) up -d --wait

down: ## Arrête la pile et supprime conteneurs, réseaux et volumes
	$(COMPOSE) --profile obs down --volumes --remove-orphans

logs: ## Suit les journaux de la pile
	$(COMPOSE) logs -f --tail=200

seed: ## Charge le jeu de données FICTIF de l'étape 1.1 dans la pile locale (rejouable)
	$(PYTHON) -I testing/fixtures/seed/seed.py

seed-test: ## Vérifie que make seed est complet et rejouable (pile démarrée)
	$(PYTHON) -I testing/fixtures/seed/test_seed.py

spike-test: ## Tests d'exploration Phase 1 contre la pile (make up && make seed) : make spike-test [SPIKE=IdempotencySpikeIT]
	$(FINERACT_DIR)/gradlew --no-daemon -p testing/spikes spikeTest $(if $(SPIKE),--tests 'cm.nkulkap.spikes.$(SPIKE)')

lint-contracts: ## Valide les contrats OpenAPI (Redocly) et les schémas d'événements Avro (Node >= 22.12)
	cd contracts && npm ci --no-audit --no-fund && npm run lint

fixtures: ## Génère les données de test FICTIVES reproductibles dans testing/fixtures/out/<SIZE>/ : make fixtures SIZE=small|large
	$(PYTHON) -I testing/fixtures/generator/generate.py $(SIZE)

fixtures-load: ## Charge testing/fixtures/out/<SIZE>/ dans Fineract par l'API, rejouable (pile démarrée) : make fixtures-load SIZE=small
	$(PYTHON) -I testing/fixtures/generator/load.py $(SIZE)

fixtures-test: ## Teste le générateur de fixtures (sans pile)
	$(PYTHON) -I testing/fixtures/generator/test_generator.py

toxiproxy: ## Applique un profil réseau dégradé à Fineract (pile démarrée) : make toxiproxy PROFILE=edge_rural|reset|list
	cd testing/toxiproxy && go run ./cmd/toxictl init >/dev/null && 		case "$(PROFILE)" in reset|list) go run ./cmd/toxictl $(PROFILE) ;; "") echo "Préciser PROFILE=<profil|reset|list>." >&2; exit 2 ;; *) go run ./cmd/toxictl apply $(PROFILE) ;; esac

toxiproxy-test: ## Tests du harnais réseau ; reponse_perdue contre la pile (make up && make seed)
	cd testing/toxiproxy && go vet ./... && NKULKAP_STACK=1 go test -count=1 -v ./...
