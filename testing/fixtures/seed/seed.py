# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Charge dans Fineract un jeu de données FICTIF minimal (étape 1.1).

Rejouable : chaque objet est recherché (externalId, code ou nom) avant création ;
un second passage ne crée rien et ne poste aucun mouvement. Les mouvements
(dépôts, décaissement) portent une Idempotency-Key dérivée de l'externalId.

Usage : python testing/fixtures/seed/seed.py   (variables : FINERACT_URL, FINERACT_TENANT,
FINERACT_USER, FINERACT_PASSWORD, FINERACT_TLS_VERIFY)
"""

from __future__ import annotations

import json
import sys
import uuid
from datetime import date
from pathlib import Path
from urllib.parse import quote

sys.path.insert(0, str(Path(__file__).resolve().parent))
from fineract_client import Fineract, FineractError  # noqa: E402

DATA = json.loads((Path(__file__).resolve().parent / "seed-data.json").read_text(encoding="utf-8"))
DATE_FMT = {"dateFormat": "yyyy-MM-dd", "locale": "fr"}
SEED_NS = uuid.UUID("6f1d0c3e-6b8a-4a7e-9a59-4b1e2c0d5a11")


def key_for(name: str) -> str:
    """Idempotency-Key stable pour un mouvement du seed (UUIDv5 de son nom)."""
    return str(uuid.uuid5(SEED_NS, name))


def log(msg: str) -> None:
    # Console Windows en cp1252 par défaut : les accents seraient illisibles.
    sys.stdout.reconfigure(encoding="utf-8")
    print(msg, flush=True)


class Seeder:
    def __init__(self, api: Fineract) -> None:
        self.api = api
        self.opening = DATA["openingDate"]
        self.today = date.today().isoformat()
        self.offices: dict[str, int] = {}
        self.staff: dict[str, int] = {}
        self.gl: dict[str, int] = {}
        self.clients: dict[str, int] = {}

    def run(self) -> None:
        self.seed_currency()
        self.seed_external_events()
        self.seed_offices()
        self.seed_staff()
        self.seed_gl_accounts()
        savings_product = self.seed_savings_product()
        loan_product = self.seed_loan_product()
        self.seed_clients()
        self.seed_savings_accounts(savings_product)
        self.seed_loan(loan_product)
        log("Seed terminé.")

    # --- Référentiel -------------------------------------------------------

    def seed_currency(self) -> None:
        # Sans XAF parmi les devises de l'organisation, POST /journalentries renvoie 404 (currency.invalid.or.not.supported).
        codes = [c["code"] for c in self.api.get("/currencies").get("selectedCurrencyOptions", [])]
        if "XAF" not in codes:
            self.api.put("/currencies", {"currencies": codes + ["XAF"]})
            log("Devise de l'organisation ajoutée : XAF")

    def seed_external_events(self) -> None:
        # Étape 1.3 : événements externes lus par NKULKAP (liste identique à ExternalEventsSpikeIT.NKULKAP_EVENTS).
        current = {e["type"]: e["enabled"] for e in self.api.get("/externalevents/configuration")["externalEventConfiguration"]}
        wanted = {t: True for t in DATA["externalEvents"] if not current.get(t)}
        unknown = [t for t in DATA["externalEvents"] if t not in current]
        if unknown:
            # Un type inconnu ferait répondre 500 au PUT : échouer avec un message clair.
            raise FineractError("PUT", "/externalevents/configuration", 0, f"types d'événements inconnus : {unknown}")
        if wanted:
            self.api.put("/externalevents/configuration", {"externalEventConfigurations": wanted})
            log(f"Événements externes activés : {', '.join(wanted)}")

    def seed_offices(self) -> None:
        existing = {o.get("externalId"): o["id"] for o in self.api.get("/offices")}
        for o in DATA["offices"]:
            if o["externalId"] in existing:
                self.offices[o["externalId"]] = existing[o["externalId"]]
                continue
            body = {"name": o["name"], "externalId": o["externalId"], "parentId": 1, "openingDate": self.opening, **DATE_FMT}
            self.offices[o["externalId"]] = self.api.post("/offices", body)["resourceId"]
            log(f"Agence créée : {o['name']}")

    def seed_staff(self) -> None:
        existing = {s.get("externalId"): s["id"] for s in self.api.get("/staff?status=all")}
        for s in DATA["staff"]:
            if s["externalId"] in existing:
                self.staff[s["externalId"]] = existing[s["externalId"]]
                continue
            body = {
                "officeId": self.offices[s["office"]],
                "firstname": s["firstname"],
                "lastname": s["lastname"],
                "externalId": s["externalId"],
                "isLoanOfficer": s["isLoanOfficer"],
                "isActive": True,
                "joiningDate": self.opening,
                **DATE_FMT,
            }
            self.staff[s["externalId"]] = self.api.post("/staff", body)["resourceId"]
            log(f"Employé créé : {s['firstname']} {s['lastname']}")

    def seed_gl_accounts(self) -> None:
        existing = {a["glCode"]: a["id"] for a in self.api.get("/glaccounts")}
        for a in DATA["glAccounts"]:
            if a["glCode"] in existing:
                self.gl[a["key"]] = existing[a["glCode"]]
                continue
            body = {"name": a["name"], "glCode": a["glCode"], "type": a["type"], "usage": 1, "manualEntriesAllowed": True}
            self.gl[a["key"]] = self.api.post("/glaccounts", body)["resourceId"]
            log(f"Compte comptable créé : {a['glCode']} {a['name']}")

    def seed_savings_product(self) -> int:
        p = DATA["savingsProduct"]
        for existing in self.api.get("/savingsproducts"):
            if existing["name"] == p["name"]:
                return existing["id"]
        g = self.gl
        body = {
            "name": p["name"],
            "shortName": p["shortName"],
            "currencyCode": "XAF",
            "digitsAfterDecimal": 0,
            "inMultiplesOf": 1,
            "nominalAnnualInterestRate": 0,
            "interestCompoundingPeriodType": 1,
            "interestPostingPeriodType": 4,
            "interestCalculationType": 1,
            "interestCalculationDaysInYearType": 365,
            "withdrawalFeeForTransfers": False,
            "allowOverdraft": False,
            "accountingRule": 2,
            "savingsReferenceAccountId": g["cash"],
            "overdraftPortfolioControlId": g["overdraftPortfolio"],
            "savingsControlAccountId": g["savingsControl"],
            "transfersInSuspenseAccountId": g["savingsTransfersSuspense"],
            "interestOnSavingsAccountId": g["savingsInterestExpense"],
            "writeOffAccountId": g["writeOff"],
            "incomeFromFeeAccountId": g["feeIncome"],
            "incomeFromPenaltyAccountId": g["penaltyIncome"],
            "incomeFromInterestId": g["interestIncome"],
            "locale": "fr",
        }
        pid = self.api.post("/savingsproducts", body)["resourceId"]
        log(f"Produit d'épargne créé : {p['name']}")
        return pid

    def seed_loan_product(self) -> int:
        p = DATA["loanProduct"]
        for existing in self.api.get("/loanproducts"):
            if existing["name"] == p["name"]:
                return existing["id"]
        g = self.gl
        body = {
            "name": p["name"],
            "shortName": p["shortName"],
            "currencyCode": "XAF",
            "digitsAfterDecimal": 0,
            "inMultiplesOf": 1,
            "principal": p["principal"],
            "minPrincipal": p["minPrincipal"],
            "maxPrincipal": p["maxPrincipal"],
            "numberOfRepayments": p["numberOfRepayments"],
            "repaymentEvery": 1,
            "repaymentFrequencyType": 2,
            "interestRatePerPeriod": p["interestRatePerPeriod"],
            "interestRateFrequencyType": 2,
            "amortizationType": 1,
            "interestType": 1,
            "interestCalculationPeriodType": 1,
            "transactionProcessingStrategyCode": "mifos-standard-strategy",
            "daysInYearType": 1,
            "daysInMonthType": 1,
            "isInterestRecalculationEnabled": False,
            "loanScheduleType": "CUMULATIVE",
            "accountingRule": 2,
            "fundSourceAccountId": g["cash"],
            "loanPortfolioAccountId": g["loanPortfolio"],
            "transfersInSuspenseAccountId": g["loanTransfersSuspense"],
            "interestOnLoanAccountId": g["interestIncome"],
            "incomeFromFeeAccountId": g["feeIncome"],
            "incomeFromPenaltyAccountId": g["penaltyIncome"],
            "incomeFromRecoveryAccountId": g["recoveryIncome"],
            "writeOffAccountId": g["writeOff"],
            "overpaymentLiabilityAccountId": g["overpayment"],
            "locale": "fr",
        }
        pid = self.api.post("/loanproducts", body)["resourceId"]
        log(f"Produit de prêt créé : {p['name']}")
        return pid

    # --- Clients et comptes ------------------------------------------------

    def find_client(self, external_id: str) -> int | None:
        page = self.api.get(f"/clients?externalId={quote(external_id)}")
        items = page.get("pageItems", [])
        return items[0]["id"] if items else None

    def seed_clients(self) -> None:
        for c in DATA["clients"]:
            cid = self.find_client(c["externalId"])
            if cid is None:
                body = {
                    "officeId": self.offices[c["office"]],
                    "legalFormId": 1,
                    "firstname": c["firstname"],
                    "lastname": c["lastname"],
                    "externalId": c["externalId"],
                    "active": True,
                    "activationDate": self.opening,
                    "submittedOnDate": self.opening,
                    **DATE_FMT,
                }
                cid = self.api.post("/clients", body)["resourceId"]
                log(f"Client créé : {c['firstname']} {c['lastname']}")
            self.clients[c["externalId"]] = cid

    def seed_savings_accounts(self, product_id: int) -> None:
        for c in DATA["clients"]:
            ext = c["externalId"].replace("SEED-CLI", "SEED-EPA")
            acc = self.find(f"/savingsaccounts/external-id/{quote(ext)}")
            if acc is None:
                body = {
                    "clientId": self.clients[c["externalId"]],
                    "productId": product_id,
                    "externalId": ext,
                    "submittedOnDate": self.opening,
                    **DATE_FMT,
                }
                sid = self.api.post("/savingsaccounts", body)["savingsId"]
                self.api.post(f"/savingsaccounts/{sid}?command=approve", {"approvedOnDate": self.opening, **DATE_FMT})
                self.api.post(f"/savingsaccounts/{sid}?command=activate", {"activatedOnDate": self.opening, **DATE_FMT})
                log(f"Compte d'épargne ouvert : {ext}")
            else:
                sid = acc["id"]
            # Dépôt initial : la clé d'idempotence empêche un second dépôt au rejeu.
            body = {
                "transactionDate": self.opening,
                "transactionAmount": c["deposit"],
                "paymentTypeId": self.cash_payment_type(),
                "externalId": f"{ext}-DEP-1",
                **DATE_FMT,
            }
            self.post_once(f"/savingsaccounts/{sid}/transactions?command=deposit", body, f"{ext}-DEP-1")

    def seed_loan(self, product_id: int) -> None:
        loan = DATA["loan"]
        path = f"/loans/external-id/{quote(loan['externalId'])}"
        found = self.find(path)
        if found is not None:
            lid = found["id"]
        else:
            client_id = self.clients[loan["client"]]
            p = DATA["loanProduct"]
            body = {
                "clientId": client_id,
                "productId": product_id,
                "externalId": loan["externalId"],
                "loanType": "individual",
                "principal": loan["principal"],
                "loanTermFrequency": p["numberOfRepayments"],
                "loanTermFrequencyType": 2,
                "numberOfRepayments": p["numberOfRepayments"],
                "repaymentEvery": 1,
                "repaymentFrequencyType": 2,
                "interestRatePerPeriod": p["interestRatePerPeriod"],
                "amortizationType": 1,
                "interestType": 1,
                "interestCalculationPeriodType": 1,
                "transactionProcessingStrategyCode": "mifos-standard-strategy",
                "loanOfficerId": self.staff["SEED-EMP-01"],
                "expectedDisbursementDate": self.opening,
                "submittedOnDate": self.opening,
                **DATE_FMT,
            }
            lid = self.api.post("/loans", body)["loanId"]
            self.api.post(f"/loans/{lid}?command=approve", {"approvedOnDate": self.opening, "approvedLoanAmount": loan["principal"], **DATE_FMT})
            log(f"Prêt créé et approuvé : {loan['externalId']}")
        status = self.api.get(f"/loans/{lid}")["status"]
        if status.get("active"):
            return
        body = {"actualDisbursementDate": self.opening, "transactionAmount": loan["principal"], "paymentTypeId": self.cash_payment_type(), **DATE_FMT}
        self.post_once(f"/loans/{lid}?command=disburse", body, f"{loan['externalId']}-DISB")
        log(f"Prêt décaissé : {loan['externalId']}")

    # --- Outils ------------------------------------------------------------

    _cash_payment_type: int | None = None

    def cash_payment_type(self) -> int:
        if self._cash_payment_type is None:
            types = self.api.get("/paymenttypes")
            cash = next((t for t in types if t.get("isCashPayment")), None)
            if cash is None:
                cash = {"id": self.api.post("/paymenttypes", {"name": "FICTIF Espèces", "isCashPayment": True, "position": 1})["resourceId"]}
                log("Type de paiement créé : FICTIF Espèces")
            self._cash_payment_type = cash["id"]
        return self._cash_payment_type

    def find(self, path: str) -> dict | None:
        """Lit une ressource ; None si Fineract répond 404."""
        try:
            return self.api.get(path)
        except FineractError as e:
            if e.status == 404:
                return None
            raise

    def post_once(self, path: str, body: dict, name: str) -> None:
        """Poste un mouvement avec une Idempotency-Key stable ; un rejeu renvoie la réponse en cache."""
        status, headers, _ = self.api.request("POST", path, body, {"Idempotency-Key": key_for(name)})
        cached = {k.lower(): v for k, v in headers.items()}.get("x-served-from-cache") == "true"
        if not cached:
            log(f"Mouvement posté : {name} (HTTP {status})")


def main() -> int:
    try:
        Seeder(Fineract()).run()
    except FineractError as e:
        sys.stderr.reconfigure(encoding="utf-8")
        print(f"Échec du seed : {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
