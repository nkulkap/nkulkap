# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Test d'intégration de l'étape 1.1 : le seed est complet et rejouable sans doublon.

Exige la pile locale démarrée (make up). Lancement : make seed-test
"""

from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from fineract_client import Fineract  # noqa: E402
from seed import DATA, Seeder  # noqa: E402


def snapshot(api: Fineract) -> dict:
    """État observable du jeu FICTIF : objets SEED-* et mouvements qui leur sont rattachés."""
    savings = {}
    for c in DATA["clients"]:
        ext = c["externalId"].replace("SEED-CLI", "SEED-EPA")
        acc = api.get(f"/savingsaccounts/external-id/{ext}?associations=transactions")
        savings[ext] = (int(acc["summary"]["accountBalance"]), len(acc.get("transactions", [])))
    loan = api.get(f"/loans/external-id/{DATA['loan']['externalId']}?associations=transactions")
    disbursals = [t for t in loan.get("transactions", []) if t["type"].get("disbursement")]
    return {
        "offices": sorted(o["externalId"] for o in api.get("/offices") if str(o.get("externalId", "")).startswith("SEED-")),
        "staff": sorted(s["externalId"] for s in api.get("/staff?status=all") if str(s.get("externalId", "")).startswith("SEED-")),
        "glCodes": sorted(a["glCode"] for a in api.get("/glaccounts") if a["glCode"].startswith("FICTIF-")),
        "savingsProducts": [p["name"] for p in api.get("/savingsproducts") if p["name"] == DATA["savingsProduct"]["name"]],
        "loanProducts": [p["name"] for p in api.get("/loanproducts") if p["name"] == DATA["loanProduct"]["name"]],
        "clients": sorted(
            c["externalId"] for c in api.get("/clients?limit=1000")["pageItems"] if str(c.get("externalId", "")).startswith("SEED-")
        ),
        "savings": savings,
        "loan": (loan["status"]["value"], len(disbursals), int(loan["principal"])),
        "seedDisbursalJournal": api.get(f"/journalentries?loanId={loan['id']}&limit=1000")["totalFilteredRecords"] >= 2,
    }


class SeedTest(unittest.TestCase):
    def test_seed_is_complete_and_replayable(self) -> None:
        api = Fineract()
        Seeder(api).run()
        first = snapshot(api)

        self.assertEqual(2, len(first["offices"]))
        self.assertEqual(3, len(first["staff"]))
        self.assertEqual(len(DATA["glAccounts"]), len(first["glCodes"]))
        self.assertEqual(1, len(first["savingsProducts"]))
        self.assertEqual(1, len(first["loanProducts"]))
        self.assertEqual(10, len(first["clients"]))
        self.assertEqual(10, len(first["savings"]))
        for c in DATA["clients"]:
            balance, count = first["savings"][c["externalId"].replace("SEED-CLI", "SEED-EPA")]
            self.assertGreaterEqual(count, 1)
            self.assertGreaterEqual(balance, 0)
        self.assertEqual(("Active", 1, DATA["loan"]["principal"]), first["loan"])
        self.assertTrue(first["seedDisbursalJournal"])

        Seeder(api).run()
        second = snapshot(api)
        self.assertEqual(json.dumps(first, sort_keys=True), json.dumps(second, sort_keys=True), "le rejeu a modifié l'état")


if __name__ == "__main__":
    unittest.main()
