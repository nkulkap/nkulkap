<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 297 à 353. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 6. Organisation des dépôts

Deux dépôts Git. Le premier, `nkulkap-fineract`, est un fork d'Apache Fineract qui suit une étiquette amont. Le second, `nkulkap`, est le monodépôt de tout le reste et inclut le fork comme sous-module. Les modules custom sont développés dans le monodépôt et reliés par lien symbolique dans le fork.

```text
nkulkap/                                   # monodépôt
├── CLAUDE.md                           # gabarit en annexe A
├── README.md
├── Makefile                            # bootstrap, build, test, e2e, up, down
├── docs/
│   ├── STATUS.md                       # avancement, mis à jour à chaque étape
│   ├── adr/                            # ADR-001 à ADR-0xx
│   ├── specs/                          # ce document + spécifications détaillées
│   ├── runbooks/                       # exploitation : sauvegarde, restauration, incidents
│   └── regulatory-inputs/              # textes et fichiers officiels (section 12)
├── core/
│   ├── fineract/                       # sous-module Git → nkulkap-fineract (fork)
│   └── custom-nkulkap/                    # → lien symbolique core/fineract/custom/nkulkap
│       ├── common/{core,starter}/
│       ├── coa/{core,service,starter}/         # L1
│       ├── teg/{core,service,starter}/         # L2
│       ├── collecte/{core,service,job,starter}/# L4
│       ├── tontine/{core,service,job,starter}/ # L4
│       └── reporting/{core,service,starter}/   # L7
├── services/
│   ├── nkulkap-sync/                      # Rust (Cargo workspace : api, inbox, poster, readmodel)
│   ├── nkulkap-edge/                      # Rust
│   ├── nkulkap-pay/                       # Java (Gradle multimodule : core, mtn, orange, notify, interbank, app)
│   ├── nkulkap-rules/                     # Go
│   └── nkulkap-sentinel/                  # Scala (sbt)
├── apps/
│   ├── nkulkap-field/                     # sous-module → fork de openMF/android-client (MPL-2.0)
│   └── nkulkap-web/                       # sous-module → fork de openMF/web-app (MPL-2.0)
├── rules/                              # scripts Numscript + tests
│   ├── collecte/frais_collecte.num
│   ├── momo/frais_retrait.num
│   └── account_map.yaml
├── contracts/                          # contrats partagés
│   ├── sync-v1.openapi.yaml
│   ├── pay-v1.openapi.yaml
│   ├── sentinel-v1.openapi.yaml
│   └── events/                         # schémas des événements NKULKAP
├── reports/templates/                  # gabarits d'états réglementaires (YAML)
├── simulators/
│   ├── mtn-momo/                       # WireMock + scénarios
│   └── orange-money/
├── deploy/
│   ├── compose/                        # dev, test, mono-EMF
│   ├── helm/                           # SaaS
│   └── edge/                           # image et service systemd du nœud d'agence
├── testing/
│   ├── toxiproxy/                      # profils réseau camerounais
│   ├── fixtures/                       # données FICTIVES (noms, MSISDN +237, XAF)
│   └── e2e/                            # scénarios E2E-01 à E2E-12
└── .github/workflows/
```

