<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 175 à 243. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

## 4. Architecture cible

```mermaid
flowchart LR
  subgraph Terrain
    F[nkulkap-field<br/>Android, Kotlin<br/>SQLite chiffré]
    E[nkulkap-edge<br/>Rust, nœud d'agence<br/>SQLite WAL + guichet web]
  end
  subgraph Central["Centre de données au Cameroun"]
    S[nkulkap-sync<br/>Rust<br/>boîte de réception idempotente]
    C[(nkulkap-core<br/>Fineract + custom/nkulkap<br/>Java 25)]
    P[nkulkap-pay<br/>Java<br/>MoMo, Orange, SMS, interbancaire]
    R[nkulkap-rules<br/>Go + Numscript]
    T[nkulkap-sentinel<br/>Scala 3<br/>LAB-FT]
    K{{Kafka KRaft}}
    DB[(PostgreSQL)]
    KC[Keycloak]
    W[nkulkap-web<br/>Angular]
  end
  subgraph Externes
    MTN[MTN MoMo API]
    OM[Orange Money API]
    SMS[Agrégateur SMS]
    INT[GIMAC / BEAC]
  end

  F -- "HTTPS, JSON+gzip, lots signés" --> S
  E -- "même protocole" --> S
  S -- "REST + Idempotency-Key" --> C
  W -- REST --> C
  W -- REST --> T
  W -- REST --> P
  C -- "événements Avro" --> K
  K --> S
  K --> T
  P -- "REST + Idempotency-Key" --> C
  P --> MTN & OM & SMS & INT
  C -- "calcul de répartition" --> R
  P -- "calcul de répartition" --> R
```

Pour la lisibilité, le schéma omet deux familles de liens : tous les services centraux utilisent PostgreSQL (une base par service, plus les bases de locataires de Fineract), et toutes les authentifications passent par Keycloak.

### 4.1 Flux d'une collecte hors-ligne (cas central)

```mermaid
sequenceDiagram
  participant Ag as Collecteur (nkulkap-field)
  participant Sy as nkulkap-sync
  participant Fi as Fineract
  participant Ru as nkulkap-rules
  participant Pa as nkulkap-pay (SMS)

  Ag->>Ag: Saisie de 2 000 XAF, création de op_id (UUIDv7)<br/>persisté AVANT tout réseau, reçu imprimé ou affiché
  Note over Ag: Pas de réseau : l'opération reste en file
  Ag->>Sy: POST /v1/sync/push (lot signé, gzip)
  Sy->>Sy: INSERT inbox (op_id PK) : doublon ignoré
  Sy-->>Ag: 202 (op_id ACCEPTED)
  Sy->>Fi: POST savingsaccounts/{id}/transactions?command=deposit<br/>Idempotency-Key: op_id
  Fi-->>Sy: 200 (ou réponse perdue : nouvelle tentative avec la même clé)
  Sy->>Sy: op POSTED
  Sy->>Pa: événement op.posted
  Pa->>Pa: SMS « Depot 2000 XAF recu, ref ... »
  Ag->>Sy: GET /v1/sync/pull?cursor=...
  Sy-->>Ag: statuts (POSTED) + changements de portefeuille
```

---

