<!-- Généré par scripts/split-spec.sh depuis NKULKAP_cahier_des_charges_et_plan.md, lignes 1383 à 1465. Ne pas modifier à la main : modifier la référence puis relancer le script. -->

### Annexe C — Protocole de synchronisation v1

#### C.1 Envoi d'un lot

`POST /v1/sync/push` — en-têtes : `Authorization: Bearer <jeton>`, `Content-Encoding: gzip`, `X-Nkulkap-Device: <device_id>`, `X-Nkulkap-Signature: <ed25519 base64 du corps non compressé>`.

```json
{
  "batch_id": "0192f1e4-7c3a-7b1e-9d2a-5f0c1a2b3c4d",
  "device_id": "dev_8Hk2",
  "tenant": "emf-demo",
  "sent_at_device": "2026-10-21T17:42:10+01:00",
  "ops": [
    {
      "op_id": "0192f1e2-1111-7aaa-8bbb-000000000001",
      "seq": 1287,
      "hlc": "1761065530123:0004:dev_8Hk2",
      "type": "COLLECTE_DEPOT",
      "account": {"kind": "SAVINGS", "id": 55231},
      "amount": 2000,
      "currency": "XAF",
      "created_at_device": "2026-10-21T09:12:44+01:00",
      "gps": {"lat": 3.8667, "lon": 11.5167, "acc_m": 25},
      "data": {"carnet_id": 812, "tournee_date": "2026-10-21", "code_recu": "K7Q2MZ"}
    }
  ]
}
```

Réponse `202` :

```json
{
  "batch_id": "0192f1e4-7c3a-7b1e-9d2a-5f0c1a2b3c4d",
  "server_time": "2026-10-21T17:42:11+01:00",
  "results": [
    {"op_id": "0192f1e2-1111-7aaa-8bbb-000000000001", "status": "ACCEPTED"}
  ],
  "clock_skew_ms": 840
}
```

#### C.2 Réception

`GET /v1/sync/pull?cursor=<opaque>&max_bytes=200000` renvoie :

```json
{
  "next_cursor": "c_9f2...",
  "has_more": false,
  "op_statuses": [{"op_id": "...0001", "status": "POSTED", "fineract_txn_id": 991245, "posted_at": "..."}],
  "changes": {
    "clients": [{"id": 4410, "op": "UPSERT", "data": {"...": "..."}}],
    "savings": [{"id": 55231, "op": "UPSERT", "data": {"balance": 48000, "as_of": "..."}}],
    "carnets": [],
    "config": {"limits": {"offline_per_op": 50000, "offline_per_day": 200000}}
  }
}
```

#### C.3 Types d'opérations et codes de rejet

| Type | Appel Fineract | Rejets métier possibles |
|---|---|---|
| `COLLECTE_DEPOT` | dépôt d'épargne + `nkulkap_collecte_op` | `ACCOUNT_CLOSED`, `ACCOUNT_BLOCKED`, `CARNET_INACTIVE` |
| `EPARGNE_DEPOT` | `command=deposit` | `ACCOUNT_CLOSED`, `ACCOUNT_BLOCKED` |
| `EPARGNE_RETRAIT` | `command=withdrawal` | `INSUFFICIENT_FUNDS`, `ACCOUNT_BLOCKED`, `LIMIT_EXCEEDED` |
| `PRET_REMBOURSEMENT` | `command=repayment` | `LOAN_CLOSED`, `OVERPAYMENT` |
| `TONTINE_COTISATION` | API tontine NKULKAP | `CYCLE_CLOSED`, `NOT_MEMBER` |
| `CLIENT_CREATION` | création de client (identifiant temporaire → identifiant serveur renvoyé dans `pull`) | `DUPLICATE_IDENTITY`, `KYC_INCOMPLETE` |

Rejets techniques (renvoyés immédiatement dans `results`) : `BAD_SIGNATURE`, `DEVICE_REVOKED`, `SCHEMA_INVALID`, `CLOCK_SKEW_TOO_LARGE`, `SEQ_GAP` (le lot est accepté, l'écart est signalé).

#### C.4 Politique de conflits

| Situation | Décision | Suite |
|---|---|---|
| Dépôt ou remboursement sur un compte clôturé ou bloqué | `REJECTED_NEEDS_REVIEW` | L'argent est déjà reçu : le superviseur l'affecte au compte d'attente puis à un autre compte, ou le rembourse |
| Retrait hors-ligne, solde devenu insuffisant | `REJECTED_NEEDS_REVIEW` | Le superviseur approuve par `force-withdrawal` (le client doit), ou le refuse et engage une procédure de recouvrement |
| Même client servi par deux canaux hors-ligne | Traité dans l'ordre HLC par compte | Le second retrait peut tomber dans le cas précédent |
| Opération sur un carnet réaffecté à un autre collecteur | Acceptée si le compte est actif | Alerte sentinel « collecte hors portefeuille » |
| Remboursement supérieur au restant dû | Comptabilisé jusqu'au solde ; excédent en revue | Le superviseur décide : épargne ou restitution |

