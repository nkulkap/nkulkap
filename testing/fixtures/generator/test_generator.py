# Copyright (c) 2026 Circaura Sarl. Tous droits réservés.
"""Tests du générateur de fixtures (étape 2.2), sans pile. Lancement : make fixtures-test"""

from __future__ import annotations

import json
import re
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import generate  # noqa: E402

MSISDN = re.compile(r"^\+2376999\d{5}$")


def read(out: Path, name: str) -> list[dict]:
    return [json.loads(line) for line in (out / f"{name}.jsonl").read_text(encoding="utf-8").splitlines()]


class GeneratorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls._tmp = tempfile.TemporaryDirectory()
        root = Path(cls._tmp.name)
        cls.dir_a, cls.dir_b = root / "a", root / "b"
        cls.manifest = generate.generate("small", cls.dir_a)
        generate.generate("small", cls.dir_b)
        cls.d = {name: read(cls.dir_a, name) for name in generate.FILES}

    @classmethod
    def tearDownClass(cls) -> None:
        cls._tmp.cleanup()

    def test_reproductible(self) -> None:
        for name in [f"{n}.jsonl" for n in generate.FILES] + ["manifest.json"]:
            self.assertEqual((self.dir_a / name).read_bytes(), (self.dir_b / name).read_bytes(), name)

    def test_comptes_small(self) -> None:
        c = self.manifest["counts"]
        self.assertEqual((c["offices"], c["staff"], c["clients"], c["savings"]), (2, 5, 200, 200))
        self.assertEqual(c["loans"], 60)
        self.assertEqual(self.manifest["seed"], generate.SEED)
        self.assertEqual(self.manifest["endDate"], "2026-09-30")
        self.assertEqual(self.manifest["mention"], "FICTIF")
        self.assertEqual(len(self.manifest["globalSha256"]), 64)
        self.assertEqual(set(self.manifest["sha256"]), {f"{n}.jsonl" for n in generate.FILES})
        self.assertGreater(c["transactionsByKind"]["loan_repayment"], 0)

    def test_montants_entiers_positifs_multiples_de_100(self) -> None:
        for t in self.d["transactions"]:
            self.assertIs(type(t["amount"]), int, t)
            self.assertGreater(t["amount"], 0, t)
            self.assertEqual(t["amount"] % 100, 0, t)
        for loan in self.d["loans"]:
            self.assertIs(type(loan["principal"]), int)
            self.assertTrue(50000 <= loan["principal"] <= 2000000 and loan["principal"] % 5000 == 0, loan)
        for name in generate.FILES:
            self.assertNotRegex((self.dir_a / f"{name}.jsonl").read_text(encoding="utf-8"), r"\d\.\d", name)

    def test_aucun_solde_negatif(self) -> None:
        balance: dict[str, int] = {}
        outstanding: dict[str, int] = {}
        loans = {l["externalId"]: l["principal"] * 112 // 100 for l in self.d["loans"]}
        for t in self.d["transactions"]:
            a, k = t["account"], t["kind"]
            if k in ("deposit", "withdrawal"):
                balance[a] = balance.get(a, 0) + (t["amount"] if k == "deposit" else -t["amount"])
                self.assertGreaterEqual(balance[a], 0, t)
            else:
                outstanding.setdefault(a, 0)
                if k == "loan_disbursement":
                    self.assertEqual(outstanding[a], 0)
                    outstanding[a] = loans[a]
                else:
                    outstanding[a] -= t["amount"]
                    self.assertGreaterEqual(outstanding[a], 0, t)

    def test_mouvements_apres_ouverture(self) -> None:
        opened = {s["externalId"]: s["openingDate"] for s in self.d["savings"]}
        opened.update({l["externalId"]: l["disbursementDate"] for l in self.d["loans"]})
        for t in self.d["transactions"]:
            self.assertGreaterEqual(t["date"], opened[t["account"]], t)
            self.assertLessEqual(t["date"], "2026-09-30", t)

    def test_msisdn_uniques_et_conformes(self) -> None:
        nums = [c["msisdn"] for c in self.d["clients"]]
        self.assertEqual(len(nums), len(set(nums)))
        for n in nums:
            self.assertRegex(n, MSISDN)

    def test_external_id_uniques_et_fictif(self) -> None:
        ids = [r["externalId"] for name in generate.FILES for r in self.d[name]]
        self.assertEqual(len(ids), len(set(ids)))
        for name in generate.FILES:
            for r in self.d[name]:
                self.assertTrue(r["externalId"].startswith("FICTIF-"), r)
                self.assertIs(r["fictif"], True)

    def test_part_anglophone(self) -> None:
        pct = 100 * sum(c["language"] == "en" for c in self.d["clients"]) / len(self.d["clients"])
        self.assertAlmostEqual(pct, 20, delta=5)

    def test_rattachement_agence_collecteur(self) -> None:
        collectors = {s["externalId"]: s["office"] for s in self.d["staff"]}
        for c in self.d["clients"]:
            self.assertEqual(collectors[c["collector"]], c["office"])

    def test_listes_de_noms(self) -> None:
        for g in ("francophone", "anglophone"):
            self.assertGreaterEqual(len(generate.NAMES[g]["firstnames"]), 60)
            self.assertGreaterEqual(len(generate.NAMES[g]["lastnames"]), 80)


if __name__ == "__main__":
    unittest.main()
