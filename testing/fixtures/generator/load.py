# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Charge dans Fineract, par l'API, les fixtures FICTIVES de testing/fixtures/out/<size>/ (étape 2.2).

Rejouable et reprenable : chaque objet est recherché par externalId avant création ; chaque
mouvement porte une Idempotency-Key = UUIDv5 de son externalId, utilisée aussi comme
externalId de transaction. Un fichier de reprise (.load-state.json, dans le répertoire des
fixtures) mémorise l'avancement par étape ; sans lui, tout est revérifié par recherche.

Réutilise les produits et comptes comptables de seed.py (créés s'ils manquent : `make seed`
n'est pas indispensable). Délais et nouvelles tentatives : voir fineract_client.py.

Usage : python load.py [small|large] [--out RÉPERTOIRE]   (variables FINERACT_* comme seed.py)
"""

from __future__ import annotations

import json
import os
import sys
import uuid
from pathlib import Path
from urllib.parse import quote

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "seed"))
from fineract_client import Fineract, FineractError  # noqa: E402
from seed import DATE_FMT, Seeder, log  # noqa: E402

FIXTURES_NS = uuid.UUID("2b7c6f0a-91d4-4c58-8e3b-5a6d0f4c7e22")
CHECKPOINT_EVERY = 100
STAGES = ["offices", "staff", "clients", "savings", "loans", "transactions"]


def key_for(external_id: str) -> str:
    """Idempotency-Key (et externalId de transaction) stable d'un mouvement."""
    return str(uuid.uuid5(FIXTURES_NS, external_id))


def read_jsonl(path: Path):
    with path.open(encoding="utf-8") as f:
        for line in f:
            if line.strip():
                yield json.loads(line)


class Loader:
    def __init__(self, api: Fineract, src: Path) -> None:
        self.api, self.src = api, src
        self.state_file = src / ".load-state.json"
        self.state: dict[str, int] = json.loads(self.state_file.read_text()) if self.state_file.exists() else {}
        self.seeder = Seeder(api)
        self.ids: dict[str, int] = {}
        self.payment_type = 0
        self.savings_product = 0
        self.loan_product = 0
        self.saving_open: dict[str, str] = {}  # client -> date d'ouverture du compte d'épargne

    # --- Outils ------------------------------------------------------------

    def save_state(self) -> None:
        tmp = self.state_file.with_suffix(".tmp")
        tmp.write_text(json.dumps(self.state, sort_keys=True))
        os.replace(tmp, self.state_file)

    def find(self, path: str) -> dict | None:
        return self.seeder.find(path)

    def stage(self, name: str, total: int):
        """Itère sur les lignes d'une étape en sautant celles déjà faites ; sauvegarde la reprise."""
        done = self.state.get(name, 0)
        if done >= total:
            log(f"[{name}] déjà chargé ({total})")
            return
        if done:
            log(f"[{name}] reprise à {done}/{total}")
        for i, row in enumerate(read_jsonl(self.src / f"{name}.jsonl")):
            if i < done:
                continue
            yield row
            self.state[name] = i + 1
            if (i + 1) % CHECKPOINT_EVERY == 0 or i + 1 == total:
                self.save_state()
                log(f"[{name}] {i + 1}/{total}")
        self.save_state()

    def lookup_page(self, path: str, key: str = "pageItems") -> int | None:
        page = self.api.get(path)
        items = page.get(key, []) if isinstance(page, dict) else page
        return items[0]["id"] if items else None

    def office_id(self, ext: str) -> int:
        if ext not in self.ids:
            for o in self.api.get("/offices"):
                if str(o.get("externalId", "")).startswith("FICTIF-"):
                    self.ids[o["externalId"]] = o["id"]
        return self.ids[ext]

    def staff_id(self, ext: str) -> int:
        if ext not in self.ids:
            for s in self.api.get("/staff?status=all&limit=1000"):
                if str(s.get("externalId", "")).startswith("FICTIF-"):
                    self.ids[s["externalId"]] = s["id"]
        return self.ids[ext]

    def client_id(self, ext: str) -> int:
        if ext not in self.ids:
            cid = self.lookup_page(f"/clients?externalId={quote(ext)}")
            if cid is None:
                raise FineractError("GET", "/clients", 404, f"client introuvable : {ext}")
            self.ids[ext] = cid
        return self.ids[ext]

    def account_id(self, kind: str, ext: str) -> int:
        if ext not in self.ids:
            found = self.find(f"/{kind}/external-id/{quote(ext)}")
            if found is None:
                raise FineractError("GET", f"/{kind}", 404, f"compte introuvable : {ext}")
            self.ids[ext] = found["id"]
        return self.ids[ext]

    # --- Étapes ------------------------------------------------------------

    def run(self) -> None:
        s = self.seeder
        s.seed_currency()
        s.seed_gl_accounts()
        self.savings_product = s.seed_savings_product()
        self.loan_product = s.seed_loan_product()
        self.payment_type = s.cash_payment_type()
        counts = {n: sum(1 for _ in read_jsonl(self.src / f"{n}.jsonl")) for n in STAGES}
        for row in read_jsonl(self.src / "savings.jsonl"):
            self.saving_open[row["client"]] = row["openingDate"]
        self.load_offices(counts["offices"])
        self.load_staff(counts["staff"])
        self.load_clients(counts["clients"])
        self.load_savings(counts["savings"])
        self.load_loans(counts["loans"])
        self.load_transactions(counts["transactions"])
        log("Chargement des fixtures terminé.")

    def load_offices(self, total: int) -> None:
        for o in self.stage("offices", total):
            if not self.find_office(o["externalId"]):
                body = {"name": o["name"], "externalId": o["externalId"], "parentId": 1, "openingDate": o["openingDate"], **DATE_FMT}
                self.ids[o["externalId"]] = self.api.post("/offices", body)["resourceId"]

    def find_office(self, ext: str) -> bool:
        try:
            self.office_id(ext)
            return True
        except KeyError:
            return False

    def load_staff(self, total: int) -> None:
        for s in self.stage("staff", total):
            try:
                self.staff_id(s["externalId"])
                continue
            except KeyError:
                pass
            body = {
                "officeId": self.office_id(s["office"]), "firstname": s["firstname"], "lastname": s["lastname"],
                "externalId": s["externalId"], "isLoanOfficer": s["isLoanOfficer"], "isActive": True,
                "joiningDate": s["joiningDate"], **DATE_FMT,
            }
            self.ids[s["externalId"]] = self.api.post("/staff", body)["resourceId"]

    def load_clients(self, total: int) -> None:
        for c in self.stage("clients", total):
            if self.lookup_page(f"/clients?externalId={quote(c['externalId'])}") is not None:
                continue
            opened = self.saving_open[c["externalId"]]
            body = {
                "officeId": self.office_id(c["office"]), "legalFormId": 1, "firstname": c["firstname"], "lastname": c["lastname"],
                "externalId": c["externalId"], "mobileNo": c["msisdn"], "staffId": self.staff_id(c["collector"]),
                "active": True, "activationDate": opened, "submittedOnDate": opened, **DATE_FMT,
            }
            self.api.post("/clients", body)

    def load_savings(self, total: int) -> None:
        for a in self.stage("savings", total):
            found = self.find(f"/savingsaccounts/external-id/{quote(a['externalId'])}")
            opened = a["openingDate"]
            if found is None:
                body = {
                    "clientId": self.client_id(a["client"]), "productId": self.savings_product,
                    "externalId": a["externalId"], "submittedOnDate": opened, **DATE_FMT,
                }
                sid = self.api.post("/savingsaccounts", body)["savingsId"]
                status = {}
            else:
                sid, status = found["id"], found.get("status", {})
            if not status.get("approved") and not status.get("active"):
                self.api.post(f"/savingsaccounts/{sid}?command=approve", {"approvedOnDate": opened, **DATE_FMT})
                status = {}
            if not status.get("active"):
                self.api.post(f"/savingsaccounts/{sid}?command=activate", {"activatedOnDate": opened, **DATE_FMT})
            self.ids[a["externalId"]] = sid

    def load_loans(self, total: int) -> None:
        for ln in self.stage("loans", total):
            found = self.find(f"/loans/external-id/{quote(ln['externalId'])}")
            when = ln["disbursementDate"]
            if found is None:
                n = ln["numberOfRepayments"]
                body = {
                    "clientId": self.client_id(ln["client"]), "productId": self.loan_product, "externalId": ln["externalId"],
                    "loanType": "individual", "principal": ln["principal"],
                    "loanTermFrequency": n, "loanTermFrequencyType": 2, "numberOfRepayments": n,
                    "repaymentEvery": 1, "repaymentFrequencyType": 2, "interestRatePerPeriod": 2,
                    "amortizationType": 1, "interestType": 1, "interestCalculationPeriodType": 1,
                    "transactionProcessingStrategyCode": "mifos-standard-strategy",
                    "loanOfficerId": self.staff_id(ln["loanOfficer"]),
                    "expectedDisbursementDate": when, "submittedOnDate": when, **DATE_FMT,
                }
                lid = self.api.post("/loans", body)["loanId"]
                approved = False
            else:
                lid, approved = found["id"], not found.get("status", {}).get("pendingApproval", True)
            if not approved:
                self.api.post(f"/loans/{lid}?command=approve", {"approvedOnDate": when, "approvedLoanAmount": ln["principal"], **DATE_FMT})
            self.ids[ln["externalId"]] = lid

    def load_transactions(self, total: int) -> None:
        for t in self.stage("transactions", total):
            key = key_for(t["externalId"])
            common = {"transactionAmount": t["amount"], "externalId": key, **DATE_FMT}
            kind, acc = t["kind"], t["account"]
            if kind in ("deposit", "withdrawal"):
                path = f"/savingsaccounts/{self.account_id('savingsaccounts', acc)}/transactions?command={kind}"
                body = {"transactionDate": t["date"], "paymentTypeId": self.payment_type, **common}
            elif kind == "loan_repayment":
                path = f"/loans/{self.account_id('loans', acc)}/transactions?command=repayment"
                body = {"transactionDate": t["date"], "paymentTypeId": self.payment_type, **common}
            else:
                path = f"/loans/{self.account_id('loans', acc)}?command=disburse"
                body = {"actualDisbursementDate": t["date"], "paymentTypeId": self.payment_type, **common}
            try:
                self.api.request("POST", path, body, {"Idempotency-Key": key})
            except FineractError as e:
                # ADR-016 : sur un refus d'unicité ou d'intégrité, relire par externalId au lieu d'analyser le texte.
                if e.status in (400, 403) and self.already_posted(kind, acc, key):
                    continue
                raise

    def already_posted(self, kind: str, acc: str, key: str) -> bool:
        """Le mouvement existe-t-il déjà dans Fineract (relecture par externalId, ou prêt déjà décaissé) ?"""
        if kind in ("deposit", "withdrawal"):
            sid = self.account_id("savingsaccounts", acc)
            return self.find(f"/savingsaccounts/{sid}/transactions/external-id/{quote(key)}") is not None
        lid = self.account_id("loans", acc)
        if kind == "loan_repayment":
            return self.find(f"/loans/{lid}/transactions/external-id/{quote(key)}") is not None
        return bool(self.api.get(f"/loans/{lid}").get("status", {}).get("active"))


def main(argv: list[str]) -> int:
    sys.stdout.reconfigure(encoding="utf-8")
    args = argv[1:]
    out_root = HERE.parent / "out"
    if "--out" in args:
        i = args.index("--out")
        out_root = Path(args[i + 1])
        del args[i:i + 2]
    size = args[0] if args else "small"
    src = out_root / size
    if not (src / "manifest.json").exists():
        print(f"Fixtures absentes : lancer d'abord make fixtures SIZE={size}", file=sys.stderr)
        return 2
    try:
        Loader(Fineract(), src).run()
    except FineractError as e:
        sys.stderr.reconfigure(encoding="utf-8")
        print(f"Échec du chargement : {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
