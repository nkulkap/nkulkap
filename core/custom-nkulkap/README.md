# Modules Fineract NKULKAP

Code NKULKAP qui s'exécute dans Fineract, sans modifier son cœur (ADR-002).

## Organisation

`<domaine>/<module>`, relié dans le fork sous `core/fineract/custom/nkulkap/` par `make bootstrap` (lien symbolique, ou jonction NTFS sous Windows : ADR-013). Fineract inclut automatiquement tout dossier `custom/<société>/<catégorie>/<module>` qui contient un `build.gradle` : `settings.gradle` n'est jamais modifié.

| Domaine | Ligne | Modules prévus |
|---|---|---|
| `common` | — | `core`, `starter` |
| `coa` | L1 | `core`, `service`, `starter` |
| `teg` | L2 | `core`, `service`, `starter` |
| `collecte` | L4 | `core`, `service`, `job`, `starter` |
| `tontine` | L4 | `core`, `service`, `job`, `starter` |
| `reporting` | L7 | `core`, `service`, `starter` |

`common/core` (ressources, migrations, écouteurs) et `common/starter` (auto-configuration) existent.

## Ajouter un module

1. Créer `<domaine>/<module>/build.gradle` et `dependencies.gradle` en suivant `common/core`, y compris `apply from: "${rootDir}/custom/nkulkap/nkulkap-conventions.gradle"` (ADR-015).
2. Groupe Gradle `cm.nkulkap.fineract`, archive `nkulkap-fineract-<domaine>-<module>`.
3. Tables préfixées `nkulkap_`, migrations dans `db/custom-changelog`.
4. `make build` compile tous les modules ; la CI `ci-core` se déclenche sur toute modification de ce dossier.
