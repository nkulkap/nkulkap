# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Générateur REPRODUCTIBLE de données FICTIVES (étape 2.2, cahier 10.4).

Même graine + même taille => fichiers octet pour octet identiques : un seul
random.Random(SEED), aucune lecture de l'heure ni de l'environnement, toutes les
dates relatives à END_DATE. Montants XAF entiers, multiples de 100.

Sortie : testing/fixtures/out/<size>/{offices,staff,clients,savings,loans,transactions}.jsonl
et manifest.json (graine, taille, comptes, empreintes SHA-256).

Usage : python generate.py [small|large] [--out RÉPERTOIRE]
"""

from __future__ import annotations

import hashlib
import json
import random
import sys
from datetime import date, timedelta
from pathlib import Path

HERE = Path(__file__).resolve().parent
DEFAULT_OUT = HERE.parent / "out"

SEED = 20261022
END_DATE = date(2026, 9, 30)
SIZES = {
    "small": {"offices": 2, "collectors": 5, "clients": 200, "months": 1},
    "large": {"offices": 10, "collectors": 50, "clients": 20000, "months": 12},
}
ENGLISH_SHARE_PCT = 20
LOAN_SHARE_PCT = 30
LOAN_REPAYMENTS = 6  # aligné sur le produit de prêt du seed (6 mensualités)
LOAN_INTEREST_PCT = 12  # 6 mois x 2 % (taux fixe du produit du seed)
FILES = ["offices", "staff", "clients", "savings", "loans", "transactions"]

NAMES = json.loads((HERE / "names.json").read_text(encoding="utf-8"))


def pick_k(rng: random.Random, n: int, k: int) -> list[int]:
    """k indices distincts de range(n), par mélange partiel de Fisher-Yates (stable d'une version de Python à l'autre)."""
    items = list(range(n))
    for i in range(k):
        j = i + rng.randrange(n - i)
        items[i], items[j] = items[j], items[i]
    return items[:k]


def iso(d: date) -> str:
    return d.isoformat()


def person(rng: random.Random, english: bool) -> tuple[str, str]:
    group = NAMES["anglophone" if english else "francophone"]
    return rng.choice(group["firstnames"]), rng.choice(group["lastnames"])


def build(size: str) -> dict[str, list[dict]]:
    cfg = SIZES[size]
    rng = random.Random(SEED)
    window = 30 * cfg["months"]
    start = END_DATE - timedelta(days=window)
    opening = start - timedelta(days=200)  # ouverture des agences et embauche, avant tout client
    # Préfixe propre à la taille : un petit et un grand jeu ne partagent aucun externalId dans Fineract.
    tag = "S" if size == "small" else "L"

    offices = []
    for i in range(1, cfg["offices"] + 1):
        city = NAMES["cities"][(i - 1) % len(NAMES["cities"])]
        offices.append({
            "externalId": f"FICTIF-{tag}-AG-{i:02d}",
            "name": f"FICTIF Agence {city} {i:02d}",
            "openingDate": iso(opening),
            "fictif": True,
        })

    staff = []
    by_office: dict[str, list[str]] = {o["externalId"]: [] for o in offices}
    for i in range(1, cfg["collectors"] + 1):
        office = offices[(i - 1) % len(offices)]["externalId"]
        first, last = person(rng, rng.randrange(100) < ENGLISH_SHARE_PCT)
        ext = f"FICTIF-{tag}-COL-{i:03d}"
        by_office[office].append(ext)
        staff.append({
            "externalId": ext, "office": office, "firstname": first, "lastname": last,
            "isLoanOfficer": True, "joiningDate": iso(opening), "fictif": True,
        })

    n = cfg["clients"]
    english = set(pick_k(rng, n, round(n * ENGLISH_SHARE_PCT / 100)))
    with_loan = set(pick_k(rng, n, round(n * LOAN_SHARE_PCT / 100)))
    # MSISDN fictifs +2376999XXXXX : 5 chiffres libres, uniques par tirage sans remise.
    phones = pick_k(rng, 100000, n)

    clients, savings, loans, movements = [], [], [], []
    loan_no = 0
    for idx in range(n):
        no = idx + 1
        office = offices[rng.randrange(len(offices))]["externalId"]
        collector = rng.choice(by_office[office])
        first, last = person(rng, idx in english)
        loan_date = None
        if idx in with_loan:
            # Décaissement entre (fenêtre + 120 j) et 15 j avant la fin ; épargne ouverte au plus 30 j avant.
            loan_date = END_DATE - timedelta(days=rng.randint(15, window + 120))
            open_date = loan_date - timedelta(days=rng.randint(0, 30))
        else:
            open_date = END_DATE - timedelta(days=rng.randint(0, window - 1))

        cli_ext, sav_ext = f"FICTIF-{tag}-CLI-{no:06d}", f"FICTIF-{tag}-EPA-{no:06d}"
        clients.append({
            "externalId": cli_ext, "firstname": first, "lastname": last,
            "language": "en" if idx in english else "fr",
            "msisdn": f"+2376999{phones[idx]:05d}",
            "office": office, "collector": collector, "fictif": True,
        })
        savings.append({
            "externalId": sav_ext, "client": cli_ext, "openingDate": iso(open_date), "fictif": True,
        })

        # Mouvements d'épargne : premier dépôt le jour d'ouverture, puis dépôts et retraits répartis jusqu'à END_DATE.
        span = (END_DATE - open_date).days
        months = max(1, span // 30)
        events = [(0, 0, 0, rng.randint(5, 500) * 100)]
        for k in range(1, 1 + rng.randint(1, 2) * months):
            events.append((rng.randint(0, span), 0, k, rng.randint(5, 500) * 100))
        for k in range(rng.randint(0, months)):
            events.append((rng.randint(0, span), 1, 1000 + k, rng.randint(1, 300) * 100))
        events.sort()
        balance, seq = 0, 0
        for off, kind, _, amount in events:
            if kind == 1:
                amount = min(amount, balance)  # jamais au-delà du solde simulé
                if amount < 100:
                    continue
                balance -= amount
            else:
                balance += amount
            movements.append((iso(open_date + timedelta(days=off)), sav_ext, seq, "withdrawal" if kind else "deposit", amount))
            seq += 1

        if loan_date is not None:
            loan_no += 1
            prt = f"FICTIF-{tag}-PRT-{loan_no:06d}"
            principal = rng.randint(10, 400) * 5000
            loans.append({
                "externalId": prt, "client": cli_ext, "loanOfficer": collector,
                "principal": principal, "numberOfRepayments": LOAN_REPAYMENTS,
                "disbursementDate": iso(loan_date), "fictif": True,
            })
            total = principal * (100 + LOAN_INTEREST_PCT) // 100
            instalment = total // LOAN_REPAYMENTS // 100 * 100
            movements.append((iso(loan_date), prt, 0, "loan_disbursement", principal))
            for j in range(1, LOAN_REPAYMENTS + 1):
                due = loan_date + timedelta(days=30 * j)
                if due > END_DATE:
                    break
                amount = total - instalment * (LOAN_REPAYMENTS - 1) if j == LOAN_REPAYMENTS else instalment
                movements.append((iso(due), prt, j, "loan_repayment", amount))

    movements.sort(key=lambda m: (m[0], m[1], m[2]))
    transactions = [
        {"externalId": f"FICTIF-{tag}-MVT-{i:09d}", "date": d, "account": acc, "kind": kind, "amount": amount, "fictif": True}
        for i, (d, acc, _, kind, amount) in enumerate(movements, 1)
    ]
    return {"offices": offices, "staff": staff, "clients": clients, "savings": savings, "loans": loans, "transactions": transactions}


def dumps(obj) -> str:
    return json.dumps(obj, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


def generate(size: str, out_dir: Path) -> dict:
    """Écrit les fichiers dans out_dir et renvoie le manifeste."""
    data = build(size)
    out_dir.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for name in FILES:
        raw = "".join(dumps(row) + "\n" for row in data[name]).encode("utf-8")
        (out_dir / f"{name}.jsonl").write_bytes(raw)
        hashes[f"{name}.jsonl"] = hashlib.sha256(raw).hexdigest()
    kinds: dict[str, int] = {}
    for t in data["transactions"]:
        kinds[t["kind"]] = kinds.get(t["kind"], 0) + 1
    global_hash = hashlib.sha256("".join(f"{k}:{hashes[k]}\n" for k in sorted(hashes)).encode()).hexdigest()
    cfg = SIZES[size]
    manifest = {
        "mention": "FICTIF",
        "fictif": True,
        "seed": SEED,
        "size": size,
        "endDate": iso(END_DATE),
        "startDate": iso(END_DATE - timedelta(days=30 * cfg["months"])),
        "months": cfg["months"],
        "counts": {**{name: len(data[name]) for name in FILES}, "transactionsByKind": dict(sorted(kinds.items()))},
        "sha256": hashes,
        "globalSha256": global_hash,
    }
    (out_dir / "manifest.json").write_bytes((json.dumps(manifest, sort_keys=True, indent=2, separators=(",", ": "), ensure_ascii=False) + "\n").encode("utf-8"))
    return manifest


def main(argv: list[str]) -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    args = argv[1:]
    out_root = DEFAULT_OUT
    if "--out" in args:
        i = args.index("--out")
        out_root = Path(args[i + 1])
        del args[i:i + 2]
    size = args[0] if args else "small"
    if size not in SIZES:
        print(f"Taille inconnue : {size} (attendu : {', '.join(SIZES)})", file=sys.stderr)
        return 2
    m = generate(size, out_root / size)
    print(f"Fixtures {size} générées dans {out_root / size}")
    print(f"  comptes : {json.dumps(m['counts'], ensure_ascii=False)}")
    print(f"  empreinte globale : {m['globalSha256']}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
