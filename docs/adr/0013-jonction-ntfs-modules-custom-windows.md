# ADR-013 — Jonction NTFS pour relier les modules custom sous Windows

- Statut : Accepté
- Date : 2026-10-21
- Source : étape 0.2 du cahier des charges (repli prévu si le lien symbolique pose problème)

## Contexte

L'étape 0.2 relie `core/fineract/custom/nkulkap` à `core/custom-nkulkap` par un lien symbolique créé par `make bootstrap`. Sous Windows, créer un lien symbolique exige le mode développeur ou des droits d'administrateur. Sur le poste de développement actuel, `ln -s` échoue (« Operation not permitted »). Le cahier prévoit alors une copie synchronisée, qui oblige à recopier après chaque modification et risque de compiler une version périmée.

## Décision

`make bootstrap` choisit le mode de liaison selon le système, réglable par `LINK_MODE` :
- Windows (Git Bash) : jonction NTFS (`mklink /J`), qui ne demande aucun droit particulier ;
- Linux et macOS : lien symbolique relatif ;
- `LINK_MODE=copy` : copie, en dernier recours.

## Conséquences

- Sous Windows, les modifications de `core/custom-nkulkap` sont visibles immédiatement par Gradle, sans recopie.
- Une jonction contient un chemin absolu : après déplacement du dépôt, relancer `make bootstrap` après avoir supprimé l'ancienne jonction (`cmd /c rmdir core\fineract\custom\nkulkap`, qui ne touche pas la source).
- La jonction vit dans l'arbre de travail du sous-module et n'est jamais commitée.
