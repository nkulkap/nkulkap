# NKULKAP — commandes du monodépôt. `make help` liste les cibles.
# Sous Windows, lancer depuis Git Bash avec GNU Make installé.

SHELL := bash
.SHELLFLAGS := -eu -o pipefail -c
.DEFAULT_GOAL := help

FINERACT_DIR   := core/fineract
CUSTOM_SRC     := core/custom-nkulkap
CUSTOM_LINK    := $(FINERACT_DIR)/custom/nkulkap
COMPOSE        := docker compose -f deploy/compose/dev.yml
# link : lien symbolique (défaut) ; copy : copie synchronisée (repli Windows, voir étape 0.2)
LINK_MODE      ?= link
COMPONENT      ?=
SCENARIO       ?=
SIZE           ?= small

.PHONY: help bootstrap build test e2e up down logs seed fixtures

help: ## Liste les cibles disponibles
	@grep -E '^[a-z0-9-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-10s\033[0m %s\n", $$1, $$2}'

bootstrap: ## Initialise les sous-modules et relie les modules custom à Fineract
	git submodule update --init --recursive
	@if [ ! -d "$(FINERACT_DIR)/custom" ]; then echo "Sous-module $(FINERACT_DIR) absent : voir docs/STATUS.md, étape 0.2." >&2; exit 1; fi
	@if [ "$(LINK_MODE)" = "copy" ]; then \
		rm -rf "$(CUSTOM_LINK)" && mkdir -p "$(CUSTOM_LINK)" && cp -R "$(CUSTOM_SRC)/." "$(CUSTOM_LINK)/" && echo "Copie synchronisée : $(CUSTOM_LINK)"; \
	elif [ -L "$(CUSTOM_LINK)" ]; then echo "Lien déjà présent : $(CUSTOM_LINK)"; \
	else MSYS=winsymlinks:nativestrict ln -s ../../custom-nkulkap "$(CUSTOM_LINK)" && echo "Lien créé : $(CUSTOM_LINK) -> ../../custom-nkulkap"; fi

build: ## Compile les modules Fineract NKULKAP
	cd $(FINERACT_DIR) && ./gradlew $$(./gradlew -q projects | grep -o ':custom:nkulkap:[a-z:-]*' | sed 's/$$/:build/' | tr '\n' ' ')

test: ## Lance les tests d'un composant : make test COMPONENT=<core|sync|edge|pay|rules|sentinel|field|web>
	@case "$(COMPONENT)" in \
		core) cd $(FINERACT_DIR) && ./gradlew $$(./gradlew -q projects | grep -o ':custom:nkulkap:[a-z:-]*' | sed 's/$$/:test/' | tr '\n' ' ') ;; \
		sync|edge|pay|rules|sentinel|field|web) echo "COMPONENT=$(COMPONENT) : pas encore créé (voir docs/STATUS.md)." >&2; exit 1 ;; \
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

seed: ## Charge les données de référence FICTIVES
	@echo "Données de référence pas encore définies (phase 2)." >&2; exit 1

fixtures: ## Génère les données de test FICTIVES : make fixtures SIZE=small
	@echo "Générateur de fixtures pas encore écrit (phase 2, SIZE=$(SIZE))." >&2; exit 1
