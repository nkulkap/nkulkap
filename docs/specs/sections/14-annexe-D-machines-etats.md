<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1466 à 1518. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Annexe D — Machines à états

#### D.1 Intention de paiement (`nkulkap-pay`)

```mermaid
stateDiagram-v2
  [*] --> CREATED
  CREATED --> SUBMITTED: appel opérateur envoyé
  SUBMITTED --> PENDING: 202 de l'opérateur
  SUBMITTED --> UNKNOWN: délai dépassé, aucune réponse
  PENDING --> SUCCEEDED: statut confirmé SUCCESSFUL
  PENDING --> FAILED: statut FAILED
  PENDING --> EXPIRED: client n'a pas validé
  PENDING --> UNKNOWN: > 24 h sans statut final
  UNKNOWN --> SUCCEEDED: interrogation ou rapprochement
  UNKNOWN --> FAILED: interrogation ou rapprochement
  SUCCEEDED --> POSTED: écriture Fineract (Idempotency-Key)
  FAILED --> REVERSED: CASH_OUT, contre-passation du transit
  EXPIRED --> REVERSED: CASH_OUT
  POSTED --> [*]
  REVERSED --> [*]
  FAILED --> [*]: encaissement
  EXPIRED --> [*]: encaissement
```

#### D.2 Opération synchronisée (`nkulkap-sync`)

```mermaid
stateDiagram-v2
  [*] --> ACCEPTED: inséré dans la boîte de réception
  ACCEPTED --> POSTING: travailleur
  POSTING --> POSTED: succès Fineract
  POSTING --> ACCEPTED: erreur réseau (nouvelle tentative, même clé)
  POSTING --> REJECTED_NEEDS_REVIEW: erreur métier
  REJECTED_NEEDS_REVIEW --> RESOLVED: action superviseur
  POSTED --> [*]
  RESOLVED --> [*]
```

#### D.3 Tournée de collecte

```mermaid
stateDiagram-v2
  [*] --> OUVERTE
  OUVERTE --> SOUMISE: collecteur clôture sa journée
  SOUMISE --> RAPPROCHEE: espèces comptées = total comptabilisé
  SOUMISE --> EN_ECART: écart ≠ 0
  EN_ECART --> RAPPROCHEE: écart comptabilisé et justifié (maker-checker)
  RAPPROCHEE --> [*]
```

> Une tournée ne peut passer en RAPPROCHÉE que lorsque toutes ses opérations sont POSTED ou RESOLVED.

